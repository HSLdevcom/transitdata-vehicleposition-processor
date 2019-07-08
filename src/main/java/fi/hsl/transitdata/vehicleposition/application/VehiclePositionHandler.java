package fi.hsl.transitdata.vehicleposition.application;


import com.google.transit.realtime.GtfsRealtime;
import com.typesafe.config.Config;
import fi.hsl.common.gtfsrt.FeedMessageFactory;
import fi.hsl.common.hfp.proto.Hfp;
import fi.hsl.common.pulsar.IMessageHandler;
import fi.hsl.common.pulsar.PulsarApplicationContext;
import fi.hsl.common.transitdata.TransitdataProperties;
import fi.hsl.common.transitdata.TransitdataSchema;
import org.apache.pulsar.client.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DateFormat;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static fi.hsl.transitdata.vehicleposition.application.TimeUtils.*;

public class VehiclePositionHandler implements IMessageHandler {
    private static final Logger log = LoggerFactory.getLogger(VehiclePositionHandler.class);

    private static final long HFP_PROCESSING_INTERVAL_MS = 500;

    private final Consumer<byte[]> consumer;
    private final Producer<byte[]> producer;
    private final Config config;

    private final StopStatusProcessor stopStatusProcessor;

    private final Map<String, List<Hfp.Data>> hfpQueue = new HashMap<>(1000);
    private final ScheduledExecutorService hfpQueueProcessor = Executors.newSingleThreadScheduledExecutor();

    public VehiclePositionHandler(final PulsarApplicationContext context) {
        consumer = context.getConsumer();
        producer = context.getProducer();
        config = context.getConfig();

        stopStatusProcessor = new StopStatusProcessor();

        hfpQueueProcessor.scheduleWithFixedDelay(() -> {
            Map<String, List<Hfp.Data>> copy;
            synchronized (hfpQueue) {
                copy = new HashMap<>(hfpQueue);
                hfpQueue.clear();
            }

            for (String vehicle : copy.keySet()) {
                Iterator<Hfp.Data> hfpIterator = copy.get(vehicle).iterator();
                Hfp.Data data = null;

                StopStatusProcessor.StopStatus currentStopStatus = null;
                //Go through all HFP messages to find the current stop status
                while (hfpIterator.hasNext()) {
                    data = hfpIterator.next();
                    currentStopStatus = stopStatusProcessor.getStopStatus(data);
                }

                //Ignore messages where the vehicle has no location or it has no stop status after reaching the final stop
                if (!data.getPayload().hasLat() || !data.getPayload().hasLong() || currentStopStatus == null) {
                    continue;
                }

                GtfsRealtime.VehiclePosition.Builder vp = GtfsRealtime.VehiclePosition.newBuilder();

                vp.setTimestamp(data.getPayload().getTsi());
                vp.setCurrentStatus(currentStopStatus.stopStatus);
                vp.setStopId(currentStopStatus.stopId);

                vp.setPosition(GtfsRealtime.Position.newBuilder()
                        .setLatitude((float) data.getPayload().getLat())
                        .setLongitude((float) data.getPayload().getLong())
                        .setSpeed((float) data.getPayload().getSpd())
                        .setBearing(data.getPayload().getHdg())
                        .setOdometer(data.getPayload().getOdo()));

                vp.setVehicle(GtfsRealtime.VehicleDescriptor.newBuilder()
                        .setId(data.getTopic().getUniqueVehicleId()));

                String startTime = getStartTime(data);

                vp.setTrip(GtfsRealtime.TripDescriptor.newBuilder()
                        .setScheduleRelationship(GtfsRealtime.TripDescriptor.ScheduleRelationship.SCHEDULED)
                        .setDirectionId(data.getTopic().getDirectionId() - 1)
                        .setRouteId(data.getTopic().getRouteId())
                        .setStartDate(data.getPayload().getOday())
                        .setStartTime(startTime));

                if (data.getPayload().getOccu() == 100) {
                    vp.setOccupancyStatus(GtfsRealtime.VehiclePosition.OccupancyStatus.FULL);
                }

                GtfsRealtime.FeedMessage feedMessage = FeedMessageFactory.createDifferentialFeedMessage(generateEntityId(data), vp.build(), data.getPayload().getTsi());
                try {
                    sendPulsarMessage(data.getTopic().getUniqueVehicleId(), feedMessage, data.getPayload().getTsi());
                } catch (PulsarClientException e) {
                    log.error("Failed to send Pulsar message", e);
                }
            }
        }, HFP_PROCESSING_INTERVAL_MS, HFP_PROCESSING_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    @Override
    public void handleMessage(Message message) {
        try {
            if (TransitdataSchema.hasProtobufSchema(message, TransitdataProperties.ProtobufSchema.HfpData)) {
                Hfp.Data data = Hfp.Data.parseFrom(message.getData());

                //Ignore events that are not relevant to calculating stop status
                if (data.getTopic().getEventType() != Hfp.Topic.EventType.VP &&
                        data.getTopic().getEventType() != Hfp.Topic.EventType.DUE &&
                        data.getTopic().getEventType() != Hfp.Topic.EventType.PAS &&
                        data.getTopic().getEventType() != Hfp.Topic.EventType.ARS &&
                        data.getTopic().getEventType() != Hfp.Topic.EventType.PDE) {
                    return;
                }

                synchronized (hfpQueue) {
                    hfpQueue.compute(data.getTopic().getUniqueVehicleId(), (key, list) ->  {
                        if (list == null) {
                            list = new LinkedList<>();
                        }
                        list.add(data);
                        return list;
                    });
                }
            } else {
                log.warn("Invalid protobuf schema, expecting HfpData");
            }
        } catch (Exception e) {
            log.error("Exception while handling message", e);
        } finally {
            ack(message.getMessageId());
        }
    }

    private static String generateEntityId(Hfp.Data data) {
        return data.getTopic().getUniqueVehicleId();
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

    private void sendPulsarMessage(final String vehicleId, final GtfsRealtime.FeedMessage feedMessage, long timestampMs) throws PulsarClientException {
        try {
            producer.newMessage()
                    .key(vehicleId)
                    .value(feedMessage.toByteArray())
                    .eventTime(timestampMs)
                    .property(TransitdataProperties.KEY_PROTOBUF_SCHEMA, TransitdataProperties.ProtobufSchema.GTFS_VehiclePosition.toString())
                    .send();
            log.debug("Produced a new position for vehicle {} with timestamp {}", vehicleId, timestampMs);
        } catch (PulsarClientException e) {
            log.error("Failed to send message to Pulsar", e);
            throw e;
        } catch (Exception e) {
            log.error("Failed to handle vehicle position message", e);
        }
    }
}
