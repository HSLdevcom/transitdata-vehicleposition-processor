package fi.hsl.transitdata.vehicleposition.application;


import com.google.transit.realtime.GtfsRealtime;
import com.typesafe.config.Config;
import fi.hsl.common.gtfsrt.FeedMessageFactory;
import fi.hsl.common.hfp.proto.Hfp;
import fi.hsl.common.pulsar.IMessageHandler;
import fi.hsl.common.pulsar.PulsarApplicationContext;
import fi.hsl.common.transitdata.TransitdataProperties;
import fi.hsl.common.transitdata.TransitdataSchema;
import fi.hsl.transitdata.vehicleposition.application.gtfsrt.GtfsRtGenerator;
import fi.hsl.transitdata.vehicleposition.application.utils.TripVehicleCache;
import fi.hsl.transitdata.vehicleposition.application.utils.VehicleTimestampValidator;
import org.apache.pulsar.client.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class VehiclePositionHandler implements IMessageHandler {
    private static final Logger log = LoggerFactory.getLogger(VehiclePositionHandler.class);

    private static final int LOG_THRESHOLD = 1000;

    private final Consumer<byte[]> consumer;
    private final Producer<byte[]> producer;
    private final Config config;

    private final TripVehicleCache tripVehicleCache;
    private final StopStatusProcessor stopStatusProcessor;
    private final VehicleTimestampValidator vehicleTimestampValidator;

    private long messagesProcessed = 0;
    private long messageProcessingStartTime = System.currentTimeMillis();

    public VehiclePositionHandler(final PulsarApplicationContext context) {
        consumer = context.getConsumer();
        producer = context.getProducer();
        config = context.getConfig();

        tripVehicleCache = new TripVehicleCache();
        stopStatusProcessor = new StopStatusProcessor();
        vehicleTimestampValidator = new VehicleTimestampValidator(config.getDuration("processor.vehicleposition.maxTimeDifference", TimeUnit.SECONDS));
    }

    @Override
    public void handleMessage(Message message) {
        try {
            if (TransitdataSchema.hasProtobufSchema(message, TransitdataProperties.ProtobufSchema.HfpData)) {
                Hfp.Data data = Hfp.Data.parseFrom(message.getData());

                //Ignore HFP messages that are not sent from vehicles on a journey
                if (data.getTopic().getJourneyType() != Hfp.Topic.JourneyType.journey) {
                    return;
                }

                //Ignore HFP messages that are not sent from vehicles on an ongoing journey
                if (data.getTopic().getTemporalType() != Hfp.Topic.TemporalType.ongoing) {
                    return;
                }

                //Ignore events that are not relevant to calculating stop status
                if (data.getTopic().getEventType() != Hfp.Topic.EventType.VP &&
                        data.getTopic().getEventType() != Hfp.Topic.EventType.DUE &&
                        data.getTopic().getEventType() != Hfp.Topic.EventType.PAS &&
                        data.getTopic().getEventType() != Hfp.Topic.EventType.ARS &&
                        data.getTopic().getEventType() != Hfp.Topic.EventType.PDE) {
                    log.debug("Ignoring HFP message with event type {}", data.getTopic().getEventType().toString());
                    return;
                }

                //If some other vehicle was registered for the trip, do not produce vehicle position
                if (!tripVehicleCache.registerVehicleForTrip(data.getTopic().getUniqueVehicleId(), data.getTopic().getRouteId(), data.getPayload().getOday(), data.getTopic().getStartTime(), data.getPayload().getDir())) {
                    log.debug("There was already a vehicle registered for trip {} / {} / {} / {} - not producing vehicle position message for {}", data.getTopic().getRouteId(), data.getPayload().getOday(), data.getTopic().getStartTime(), data.getPayload().getDir(), data.getTopic().getUniqueVehicleId());
                    return;
                }

                if (!vehicleTimestampValidator.validateTimestamp(data, message.getEventTime())) {
                    //Vehicle had invalid timestamp..
                    return;
                }

                StopStatusProcessor.StopStatus stopStatus = stopStatusProcessor.getStopStatus(data);
                Optional<GtfsRealtime.VehiclePosition> optionalVehiclePosition = GtfsRtGenerator.generateVehiclePosition(data, stopStatus);
                if (optionalVehiclePosition.isPresent()) {
                    final GtfsRealtime.VehiclePosition vehiclePosition = optionalVehiclePosition.get();

                    final String topicSuffix = getTopicSuffix(vehiclePosition);

                    final GtfsRealtime.FeedMessage feedMessage = FeedMessageFactory.createDifferentialFeedMessage(generateEntityId(data), vehiclePosition, data.getPayload().getTsi());

                    sendPulsarMessage(data.getTopic().getUniqueVehicleId(), topicSuffix, feedMessage, data.getPayload().getTsi());
                }
            } else {
                log.warn("Invalid protobuf schema, expecting HfpData");
            }
        } catch (Exception e) {
            log.error("Exception while handling message", e);
        } finally {
            ack(message.getMessageId());

            if (++messagesProcessed % LOG_THRESHOLD == 0) {
                log.info("{} messages processed in {}ms", LOG_THRESHOLD, System.currentTimeMillis() - messageProcessingStartTime);
                messageProcessingStartTime = System.currentTimeMillis();
            }
        }
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
