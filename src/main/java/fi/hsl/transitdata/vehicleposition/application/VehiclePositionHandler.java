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
import org.apache.pulsar.client.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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
            long startTime = System.currentTimeMillis();
            int messageCount = 0;
            log.info("Processing HFP queue");

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
                    messageCount++;

                    data = hfpIterator.next();
                    currentStopStatus = stopStatusProcessor.getStopStatus(data);
                }

                Optional<GtfsRealtime.VehiclePosition> optionalVehiclePosition = GtfsRtGenerator.generateVehiclePosition(data, currentStopStatus);
                if (optionalVehiclePosition.isPresent()) {
                    GtfsRealtime.FeedMessage feedMessage = FeedMessageFactory.createDifferentialFeedMessage(generateEntityId(data), optionalVehiclePosition.get(), data.getPayload().getTsi());
                    sendPulsarMessage(data.getTopic().getUniqueVehicleId(), feedMessage, data.getPayload().getTsi());
                }
            }

            log.info("HFP queue ({} messages) processed in {}ms", messageCount, System.currentTimeMillis() - startTime);
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
                    log.debug("Ignoring HFP message with event type {}", data.getTopic().getEventType().toString());
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

    private void sendPulsarMessage(final String vehicleId, final GtfsRealtime.FeedMessage feedMessage, long timestampMs) {
        producer.newMessage()
            .key(vehicleId)
            .value(feedMessage.toByteArray())
            .eventTime(timestampMs)
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
