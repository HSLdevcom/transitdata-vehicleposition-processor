package fi.hsl.transitdata.vehicleposition.application;


import com.google.protobuf.InvalidProtocolBufferException;
import com.google.transit.realtime.GtfsRealtime;
import com.typesafe.config.Config;
import fi.hsl.common.gtfsrt.FeedMessageFactory;
import fi.hsl.common.hfp.proto.Hfp;
import fi.hsl.common.passengercount.proto.PassengerCount;
import fi.hsl.common.pulsar.IMessageHandler;
import fi.hsl.common.pulsar.PulsarApplicationContext;
import fi.hsl.common.transitdata.TransitdataProperties;
import fi.hsl.common.transitdata.TransitdataSchema;
import fi.hsl.transitdata.vehicleposition.application.gtfsrt.GtfsRtGenerator;
import fi.hsl.transitdata.vehicleposition.application.gtfsrt.GtfsRtOccupancyStatusHelper;
import fi.hsl.transitdata.vehicleposition.application.utils.PassengerCountCache;
import fi.hsl.transitdata.vehicleposition.application.utils.TripVehicleCache;
import fi.hsl.transitdata.vehicleposition.application.utils.VehicleDelayValidator;
import fi.hsl.transitdata.vehicleposition.application.utils.VehicleTimestampValidator;
import org.apache.pulsar.client.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class VehiclePositionHandler implements IMessageHandler {
    private static final Logger log = LoggerFactory.getLogger(VehiclePositionHandler.class);

    private static final Duration LOG_INTERVAL = Duration.ofSeconds(1);
    //If HFP message is delayed by more than this duration, add it to delayed messages counter
    private static final Duration DELAYED_MESSAGE_THRESHOLD = Duration.ofMinutes(2);

    private final Consumer<byte[]> consumer;
    private final Producer<byte[]> producer;
    private final Config config;

    private final TripVehicleCache tripVehicleCache;
    private final StopStatusProcessor stopStatusProcessor;
    private final VehicleTimestampValidator vehicleTimestampValidator;

    private final VehicleDelayValidator vehicleDelayValidator;

    private final GtfsRtOccupancyStatusHelper gtfsRtOccupancyStatusHelper;

    private long messagesProcessed = 0;
    private long messagesDelayed = 0;
    private long messageProcessingStartTime = System.nanoTime();

    //Keeps track of latest passenger count message received for the trip
    private final PassengerCountCache passengerCountCache = new PassengerCountCache();

    private final Set<Hfp.Topic.TransportMode> addedTripsEnabledModes;

    public VehiclePositionHandler(final PulsarApplicationContext context) {
        consumer = context.getConsumer();
        producer = context.getSingleProducer();
        config = context.getConfig();

        tripVehicleCache = new TripVehicleCache();
        stopStatusProcessor = new StopStatusProcessor();
        vehicleTimestampValidator = new VehicleTimestampValidator(config.getDuration("processor.vehicleposition.maxTimeDifference", TimeUnit.SECONDS));
        vehicleDelayValidator = new VehicleDelayValidator(config.getDuration("processor.vehicleposition.maxDelayAllowed", TimeUnit.SECONDS));

        addedTripsEnabledModes = Arrays.stream(config.getString("processor.vehicleposition.addedTripEnabledModes").split(","))
                .map(Hfp.Topic.TransportMode::valueOf)
                .collect(Collectors.toSet());

        NavigableMap<Integer, GtfsRealtime.VehiclePosition.OccupancyStatus> occupancyStatusMap = config.getConfigList("processor.vehicleposition.occuLevels")
                .stream()
                .collect(
                        TreeMap::new,
                        (map, config) -> map.put(config.getInt("occu"), GtfsRealtime.VehiclePosition.OccupancyStatus.valueOf(config.getString("status"))),
                        TreeMap::putAll
                );

        NavigableMap<Double, GtfsRealtime.VehiclePosition.OccupancyStatus> occuLevelsVehicleLoadRatio = config.getConfigList("processor.vehicleposition.occuLevelsVehicleLoadRatio")
                .stream()
                .collect(
                        TreeMap::new,
                        (map, config) -> map.put(config.getDouble("loadRatio"), GtfsRealtime.VehiclePosition.OccupancyStatus.valueOf(config.getString("status"))),
                        TreeMap::putAll
                );

        List<String> passengerCountEnabledVehicles = Arrays.stream(config.getString("processor.vehicleposition.passengerCountEnabledVehicles").split(","))
                .filter(Predicate.not(String::isBlank))
                .collect(Collectors.toList());

        gtfsRtOccupancyStatusHelper = new GtfsRtOccupancyStatusHelper(occupancyStatusMap, occuLevelsVehicleLoadRatio, passengerCountEnabledVehicles);
    }

    private static String getUniqueVehicleId(int oper, int veh) {
        return oper + "_" + veh;
    }

    @Override
    public void handleMessage(Message message) {
        try {
            if (TransitdataSchema.hasProtobufSchema(message, TransitdataProperties.ProtobufSchema.PassengerCount)) {
                PassengerCount.Data data = null;
                try {
                    data = PassengerCount.Data.parseFrom(message.getData());
                } catch (InvalidProtocolBufferException e) {
                    log.error("Failed to parse passenger count data", e);
                    throw new Exception(e);
                }
                
                try {
                    final String uniqueVehicleId = getUniqueVehicleId(data.getPayload().getOper(), data.getPayload().getVeh());
    
                    passengerCountCache.updatePassengerCount(uniqueVehicleId, data.getPayload().getRoute(), data.getPayload().getOday(), data.getPayload().getStart(), data.getPayload().getDir(), data.getPayload());
                } catch (Exception x) {
                    log.error("Failed to get unique vehicleId and update passenger count");
                    throw x;
                }
            } else if (TransitdataSchema.hasProtobufSchema(message, TransitdataProperties.ProtobufSchema.HfpData)) {
                Hfp.Data data = null;
                try {
                    data = Hfp.Data.parseFrom(message.getData());
                } catch (InvalidProtocolBufferException e) {
                    log.error("Failed to parse HfpData", e);
                    throw new Exception(e);
                }
                
                try {
                    //Ignore HFP messages that are not sent from vehicles on a journey
                    if (data.getTopic().getJourneyType() != Hfp.Topic.JourneyType.journey) {
                        // This log statement is commented out because it produces a big amount of log items
                        //log.info("Ignore HFP messages that are not sent from vehicles on a journey");
                        return;
                    }
    
                    //Ignore HFP messages that are not sent from vehicles on an ongoing journey
                    if (data.getTopic().getTemporalType() != Hfp.Topic.TemporalType.ongoing) {
                        // This log statement is commented out because it produces a big amount of log items
                        //log.info("Ignored message since vehicle wasn't on a journey");
                        return;
                    }
    
                    //Ignore events that are not relevant to calculating stop status
                    if (data.getTopic().getEventType() != Hfp.Topic.EventType.VP &&
                            data.getTopic().getEventType() != Hfp.Topic.EventType.DUE &&
                            data.getTopic().getEventType() != Hfp.Topic.EventType.PAS &&
                            data.getTopic().getEventType() != Hfp.Topic.EventType.ARS &&
                            data.getTopic().getEventType() != Hfp.Topic.EventType.PDE) {
                        log.debug("Ignoring HFP message with event type {}", data.getTopic().getEventType());
                        return;
                    }
                } catch (Exception x) {
                    log.error("Topic related checks failed");
                    throw x;
                }

                final boolean tripAlreadyTaken = !tripVehicleCache.registerVehicleForTrip(data.getTopic().getUniqueVehicleId(), data.getTopic().getRouteId(), data.getPayload().getOday(), data.getTopic().getStartTime(), data.getPayload().getDir());
                
                try {
                    if (tripAlreadyTaken && !addedTripsEnabledModes.contains(data.getTopic().getTransportMode())) {
                        //If some other vehicle was registered for the trip and the vehicle is not a bus, do not produce vehicle position
                        log.debug("There was already a vehicle registered for trip {} / {} / {} / {} - not producing vehicle position message for {}", data.getTopic().getRouteId(), data.getPayload().getOday(), data.getTopic().getStartTime(), data.getPayload().getDir(), data.getTopic().getUniqueVehicleId());
                        return;
                    }
                } catch (Exception x) {
                    log.error("tripAlreadyTaken check failed");
                    throw x;
                }
                
                try {
                    if (!vehicleTimestampValidator.validateTimestamp(data, message.getEventTime())) {
                        //Vehicle had invalid timestamp..
                        return;
                    }
    
                    if (!vehicleDelayValidator.validateDelay(data)) {
                        // Vehicle was delayed too much
                        return;
                    }
                } catch (Exception x) {
                    log.error("Validations failed");
                    throw x;
                }
                
                String detailMessage = "";
                
                try {
                    StopStatusProcessor.StopStatus stopStatus = stopStatusProcessor.getStopStatus(data);
                    detailMessage = "stopStatusProcessor.getStopStatus(data) called";
                    String uniqueVehicleId = getUniqueVehicleId(data.getTopic().getOperatorId(), data.getTopic().getVehicleNumber());
                    detailMessage = "getUniqueVehicleId called";
                    PassengerCount.Payload passengerCount = passengerCountCache.getPassengerCount(uniqueVehicleId, data.getPayload().getRoute(), data.getPayload().getOday(), data.getPayload().getStart(), data.getPayload().getDir());
                    detailMessage = "passengerCountCache.getPassengerCount called";
                    if (!isValidPassengerCountData(passengerCount)) {
                        if (passengerCount != null) {
                            log.warn("Passenger count for vehicle {} was invalid (vehicle load: {}, vehicle load ratio: {})",
                                    uniqueVehicleId,
                                    passengerCount.getVehicleCounts().getVehicleLoad(),
                                    passengerCount.getVehicleCounts().getVehicleLoadRatio());
                        }
        
                        //Don't use invalid data
                        passengerCount = null;
                    }
                    detailMessage = "isValidPassengerCountData(passengerCount) called";
    
                    Optional<GtfsRealtime.VehiclePosition.OccupancyStatus> maybeOccupancyStatus = gtfsRtOccupancyStatusHelper.getOccupancyStatus(data.getPayload(), passengerCount);
                    detailMessage = "gtfsRtOccupancyStatusHelper.getOccupancyStatus called";
                    Optional<GtfsRealtime.VehiclePosition> optionalVehiclePosition = GtfsRtGenerator.generateVehiclePosition(data, tripAlreadyTaken ? GtfsRealtime.TripDescriptor.ScheduleRelationship.ADDED : GtfsRealtime.TripDescriptor.ScheduleRelationship.SCHEDULED, stopStatus, maybeOccupancyStatus);
                    detailMessage = "GtfsRtGenerator.generateVehiclePosition called";
                    if (optionalVehiclePosition.isPresent()) {
                        detailMessage = "optionalVehiclePosition.isPresent() called";
                        final GtfsRealtime.VehiclePosition vehiclePosition = optionalVehiclePosition.get();
                        detailMessage = "optionalVehiclePosition.get() called";
                        final String topicSuffix = getTopicSuffix(vehiclePosition);
                        detailMessage = "getTopicSuffix(vehiclePosition) called";
                        final GtfsRealtime.FeedMessage feedMessage = FeedMessageFactory.createDifferentialFeedMessage(generateEntityId(data), vehiclePosition, data.getPayload().getTsi());
                        detailMessage = "ssageFactory.createDifferentialFeedMessage called";
                        if (Duration.ofMillis(System.currentTimeMillis() - (data.getPayload().getTsi() * 1000)).compareTo(DELAYED_MESSAGE_THRESHOLD) >= 0) {
                            messagesDelayed++;
                        }
                        detailMessage = "Duration.ofMillis called";
                        sendPulsarMessage(data.getTopic().getUniqueVehicleId(), topicSuffix, feedMessage, data.getPayload().getTsi());
                    }
                    detailMessage = "If there is an error, you should not see this";
                } catch (Exception x) {
                    log.error("Preparing or sending pulsar message failed. {}", detailMessage);
                    throw x;
                }
            } else {
                log.warn("Invalid protobuf schema, expecting HfpData");
            }
        } catch (Exception e) {
            log.error("Exception while handling message", e);
        } finally {
            ack(message.getMessageId());

            messagesProcessed++;

            final Duration timeSinceLastLogging = Duration.ofNanos(System.nanoTime() - messageProcessingStartTime);
            if (timeSinceLastLogging.compareTo(LOG_INTERVAL) >= 0) {
                log.info("{} messages processed during last {}ms ({} messages delayed by more than {} seconds)", messagesProcessed, timeSinceLastLogging.toMillis(), messagesDelayed, DELAYED_MESSAGE_THRESHOLD.toSeconds());

                messagesProcessed = 0;
                messagesDelayed = 0;
                messageProcessingStartTime = System.nanoTime();
            }
        }
    }

    /**
     * Checks if the passenger count data is valid (i.e. no negative passenger count etc.)
     * @param payload
     * @return
     */
    private static boolean isValidPassengerCountData(PassengerCount.Payload payload) {
        if (payload == null) {
            return false;
        }

        return payload.getVehicleCounts().getVehicleLoad() >= 0;
    }

    static String getTopicSuffix(GtfsRealtime.VehiclePosition vehiclePosition) {
        final GtfsRealtime.TripDescriptor trip = vehiclePosition.getTrip();

        return String.join("/",
                trip.getRouteId(),
                trip.getStartDate(),
                trip.getStartTime(),
                String.valueOf(trip.getDirectionId()),
                vehiclePosition.getCurrentStatus().name(),
                vehiclePosition.getStopId());
    }

    private static String generateEntityId(Hfp.Data data) {
        return "vehicle_position_"+data.getTopic().getUniqueVehicleId();
        //return String.join("_",data.getTopic().getUniqueVehicleId(), data.getTopic().getRouteId(), data.getPayload().getOday(), data.getTopic().getStartTime(), String.valueOf(data.getTopic().getDirectionId()));
    }

    private void ack(MessageId received) {
        consumer.acknowledgeAsync(received)
                .exceptionally(throwable -> {
                    log.error("Failed to ack Pulsar message", throwable);
                    return null;
                })
                .thenRun(() -> {});
    }

    private void sendPulsarMessage(final String vehicleId, final String topicSuffix, final GtfsRealtime.FeedMessage feedMessage, long timestampMs) {
        producer.newMessage()
            .key(vehicleId)
            .value(feedMessage.toByteArray())
            .eventTime(timestampMs)
            .property(TransitdataProperties.KEY_MQTT_TOPIC, topicSuffix)
            .property(TransitdataProperties.KEY_PROTOBUF_SCHEMA, TransitdataProperties.ProtobufSchema.GTFS_VehiclePosition.toString())
            .sendAsync()
            .whenComplete((messageId, error) -> {
                if (error != null) {
                    if (error instanceof PulsarClientException) {
                        log.error("Failed to send message to Pulsar", error);
                    } else {
                        log.error("Failed to handle vehicle position message", error);
                    }
                }

                if (messageId != null) {
                    log.debug("Produced a new position for vehicle {} with timestamp {}", vehicleId, timestampMs);
                }
            });
    }
}
