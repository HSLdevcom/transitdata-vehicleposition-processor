package fi.hsl.transitdata.vehicleposition.application;

import com.google.transit.realtime.GtfsRealtime;
import com.typesafe.config.Config;
import fi.hsl.common.hfp.HfpParser;
import fi.hsl.common.hfp.proto.Hfp;
import fi.hsl.common.pulsar.PulsarApplicationContext;
import fi.hsl.common.transitdata.TransitdataProperties;
import org.apache.pulsar.client.api.Consumer;
import org.apache.pulsar.client.api.Message;
import org.apache.pulsar.client.api.MessageId;
import org.apache.pulsar.client.api.Producer;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class VehiclePositionHandlerTest {
    @Test
    public void testGetTopicSuffix() {
        GtfsRealtime.VehiclePosition vehiclePosition = GtfsRealtime.VehiclePosition.newBuilder()
                .setTrip(GtfsRealtime.TripDescriptor.newBuilder()
                        .setRouteId("2550")
                        .setStartDate("20200101")
                        .setStartTime("12:00:00")
                        .setDirectionId(1))
                .setVehicle(GtfsRealtime.VehicleDescriptor.newBuilder()
                        .setId("1/1"))
                .setPosition(GtfsRealtime.Position.newBuilder()
                        .setBearing(180)
                        .setSpeed(10)
                        .setLatitude(60.513f)
                        .setLongitude(24.626f))
                .setCurrentStatus(GtfsRealtime.VehiclePosition.VehicleStopStatus.STOPPED_AT)
                .setStopId("2222212")
                .build();

        assertEquals("2550/20200101/12:00:00/1/STOPPED_AT/2222212", VehiclePositionHandler.getTopicSuffix(vehiclePosition));
    }

    @Test
    public void testAddedTrips() throws HfpParser.InvalidHfpTopicException, HfpParser.InvalidHfpPayloadException, IOException {
        final DateTimeFormatter hfpDateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

        final Instant now = Instant.now();

        final Hfp.Topic hfpTopic1 = HfpParser.parseTopic("/hfp/v2/journey/ongoing/vp/ubus/0130/07915/7280/1/Veikkola/14:30/null/2/60;24/20/59/31", now.toEpochMilli());
        final byte[] hfpPayloadBytes1 = ("{\n" +
                "  \"VP\":{\n" +
                "    \"desi\":\"280\",\n" +
                "    \"dir\":\"1\",\n" +
                "    \"oper\":130,\n" +
                "    \"veh\":7915,\n" +
                "    \"tst\":\"" + now.atOffset(ZoneOffset.UTC).format(hfpDateTimeFormatter) + "\",\n" +
                "    \"tsi\":" + now.getEpochSecond() + ",\n" +
                "    \"spd\":22.42071533203125,\n" +
                "    \"hdg\":234,\n" +
                "    \"lat\":60.25323486328125,\n" +
                "    \"long\":24.091751098632812,\n" +
                "    \"acc\":0.0,\n" +
                "    \"dl\":null,\n" +
                "    \"odo\":64314.378643035896,\n" +
                "    \"drst\":null,\n" +
                "    \"oday\":\"2022-10-27\",\n" +
                "    \"jrn\":null,\n" +
                "    \"line\":null,\n" +
                "    \"start\":\"14:30\",\n" +
                "    \"loc\":\"GPS\",\n" +
                "    \"stop\":null,\n" +
                "    \"route\":\"7280\",\n" +
                "    \"occu\":0\n" +
                "  }\n" +
                "}").getBytes(StandardCharsets.UTF_8);
        final Hfp.Payload hfpPayload1 = HfpParser.parsePayload(HfpParser.newInstance().parseJson(hfpPayloadBytes1));

        final byte[] hfpMessage1 = Hfp.Data.newBuilder().setTopic(hfpTopic1).setPayload(hfpPayload1).setSchemaVersion(1).build().toByteArray();

        final Hfp.Topic hfpTopic2 = HfpParser.parseTopic("/hfp/v2/journey/ongoing/vp/ubus/0130/07914/7280/1/Veikkola/14:30/null/2/60;24/20/59/31", now.toEpochMilli());
        final byte[] hfpPayloadBytes2 = ("{\n" +
                "  \"VP\":{\n" +
                "    \"desi\":\"280\",\n" +
                "    \"dir\":\"1\",\n" +
                "    \"oper\":130,\n" +
                "    \"veh\":7914,\n" +
                "    \"tst\":\"" + now.atOffset(ZoneOffset.UTC).format(hfpDateTimeFormatter) + "\",\n" +
                "    \"tsi\":" + now.getEpochSecond() + ",\n" +
                "    \"spd\":22.42071533203125,\n" +
                "    \"hdg\":234,\n" +
                "    \"lat\":60.25323486328125,\n" +
                "    \"long\":24.091751098632812,\n" +
                "    \"acc\":0.0,\n" +
                "    \"dl\":null,\n" +
                "    \"odo\":64314.378643035896,\n" +
                "    \"drst\":null,\n" +
                "    \"oday\":\"2022-10-27\",\n" +
                "    \"jrn\":null,\n" +
                "    \"line\":null,\n" +
                "    \"start\":\"14:30\",\n" +
                "    \"loc\":\"GPS\",\n" +
                "    \"stop\":null,\n" +
                "    \"route\":\"7280\",\n" +
                "    \"occu\":0\n" +
                "  }\n" +
                "}").getBytes(StandardCharsets.UTF_8);
        final Hfp.Payload hfpPayload2 = HfpParser.parsePayload(HfpParser.newInstance().parseJson(hfpPayloadBytes2));

        final byte[] hfpMessage2 = Hfp.Data.newBuilder().setTopic(hfpTopic2).setPayload(hfpPayload2).setSchemaVersion(1).build().toByteArray();

        final Message mockMessage1 = mock(Message.class);
        when(mockMessage1.getData()).thenReturn(hfpMessage1);
        when(mockMessage1.getProperty(TransitdataProperties.KEY_PROTOBUF_SCHEMA)).thenReturn(TransitdataProperties.ProtobufSchema.HfpData.toString());
        when(mockMessage1.getMessageId()).thenReturn(mock(MessageId.class));
        when(mockMessage1.getEventTime()).thenReturn(now.toEpochMilli());

        final Message mockMessage2 = mock(Message.class);
        when(mockMessage2.getData()).thenReturn(hfpMessage2);
        when(mockMessage2.getProperty(TransitdataProperties.KEY_PROTOBUF_SCHEMA)).thenReturn(TransitdataProperties.ProtobufSchema.HfpData.toString());
        when(mockMessage2.getMessageId()).thenReturn(mock(MessageId.class));
        when(mockMessage2.getEventTime()).thenReturn(now.toEpochMilli());

        PulsarApplicationContext mockPulsarApplicationContext = mock(PulsarApplicationContext.class);

        Consumer mockConsumer = mock(Consumer.class);
        when(mockConsumer.acknowledgeAsync(any(MessageId.class))).thenReturn(CompletableFuture.runAsync(() -> {}));

        when(mockPulsarApplicationContext.getConsumer()).thenReturn(mockConsumer);

        Producer mockProducer = mock(Producer.class);
        when(mockProducer.sendAsync(any())).thenReturn(CompletableFuture.completedFuture(MessageId.latest));

        when(mockPulsarApplicationContext.getSingleProducer()).thenReturn(mockProducer);

        Config mockConfig = mock(Config.class);
        when(mockConfig.getDuration("processor.vehicleposition.maxTimeDifference", TimeUnit.SECONDS)).thenReturn(120L);
        when(mockConfig.getString("processor.vehicleposition.addedTripEnabledModes")).thenReturn("ubus,bus");
        when(mockConfig.getString("processor.vehicleposition.occuLevels")).thenReturn("");
        when(mockConfig.getString("processor.vehicleposition.occuLevelsVehicleLoadRatio")).thenReturn("");
        when(mockConfig.getString("processor.vehicleposition.passengerCountEnabledVehicles")).thenReturn("");

        when(mockPulsarApplicationContext.getConfig()).thenReturn(mockConfig);

        final VehiclePositionHandler vehiclePositionHandler = new VehiclePositionHandler(mockPulsarApplicationContext);
        vehiclePositionHandler.handleMessage(mockMessage1);
        vehiclePositionHandler.handleMessage(mockMessage2);

        verify(mockProducer, times(2)).newMessage();
    }
}
