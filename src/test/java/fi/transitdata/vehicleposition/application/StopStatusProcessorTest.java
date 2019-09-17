package fi.transitdata.vehicleposition.application;

import com.google.transit.realtime.GtfsRealtime;
import fi.hsl.common.hfp.proto.Hfp;
import fi.hsl.transitdata.vehicleposition.application.StopStatusProcessor;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class StopStatusProcessorTest {
    private Hfp.Data generateHfpData(Hfp.Topic.EventType eventType, String nextStop, String payloadStop) {
        return generateHfpData(eventType, Hfp.Topic.TransportMode.bus, nextStop, payloadStop);
    }

    private Hfp.Data generateHfpData(Hfp.Topic.EventType eventType, Hfp.Topic.TransportMode transportMode, String nextStop, String payloadStop) {
        Hfp.Payload.Builder payload = Hfp.Payload.newBuilder();
            payload.setSchemaVersion(1);
            payload.setTst("");
            payload.setTsi(0);
            if (payloadStop != null) {
                payload.setStop(Integer.parseInt(payloadStop));
            }

        return Hfp.Data.newBuilder()
                .setSchemaVersion(1)
                .setTopic(Hfp.Topic.newBuilder()
                        .setSchemaVersion(1)
                        .setReceivedAt(0)
                        .setTopicPrefix("hfp")
                        .setTopicVersion("v2")
                        .setJourneyType(Hfp.Topic.JourneyType.journey)
                        .setTemporalType(Hfp.Topic.TemporalType.ongoing)
                        .setTransportMode(transportMode)
                        .setOperatorId(1)
                        .setVehicleNumber(1)
                        .setUniqueVehicleId("1/1")
                        .setEventType(eventType)
                        .setNextStop(nextStop))
                .setPayload(payload)
                .build();
    }

    @Test
    public void testCalculateStopStatus() {
        StopStatusProcessor calculator = new StopStatusProcessor();

        StopStatusProcessor.StopStatus stopStatus1 = calculator.getStopStatus(generateHfpData(Hfp.Topic.EventType.VP, "1", null));
        assertEquals(GtfsRealtime.VehiclePosition.VehicleStopStatus.IN_TRANSIT_TO, stopStatus1.stopStatus);
        assertEquals("1", stopStatus1.stopId);

        StopStatusProcessor.StopStatus stopStatus2 = calculator.getStopStatus(generateHfpData(Hfp.Topic.EventType.DUE, "1", "1"));
        assertEquals(GtfsRealtime.VehiclePosition.VehicleStopStatus.INCOMING_AT, stopStatus2.stopStatus);
        assertEquals("1", stopStatus2.stopId);

        StopStatusProcessor.StopStatus stopStatus3 = calculator.getStopStatus(generateHfpData(Hfp.Topic.EventType.VP, "1", "1"));
        assertEquals(GtfsRealtime.VehiclePosition.VehicleStopStatus.INCOMING_AT, stopStatus3.stopStatus);
        assertEquals("1", stopStatus3.stopId);

        StopStatusProcessor.StopStatus stopStatus4 = calculator.getStopStatus(generateHfpData(Hfp.Topic.EventType.ARS, "1", "1"));
        assertEquals(GtfsRealtime.VehiclePosition.VehicleStopStatus.STOPPED_AT, stopStatus4.stopStatus);
        assertEquals("1", stopStatus4.stopId);

        StopStatusProcessor.StopStatus stopStatus5 = calculator.getStopStatus(generateHfpData(Hfp.Topic.EventType.PDE, "2", "1"));
        assertEquals(GtfsRealtime.VehiclePosition.VehicleStopStatus.IN_TRANSIT_TO, stopStatus5.stopStatus);
        assertEquals("2", stopStatus5.stopId);

        StopStatusProcessor.StopStatus stopStatus6 = calculator.getStopStatus(generateHfpData(Hfp.Topic.EventType.VP, "2", null));
        assertEquals(GtfsRealtime.VehiclePosition.VehicleStopStatus.IN_TRANSIT_TO, stopStatus6.stopStatus);
        assertEquals("2", stopStatus6.stopId);

        StopStatusProcessor.StopStatus stopStatus7 = calculator.getStopStatus(generateHfpData(Hfp.Topic.EventType.PAS, "3", "2"));
        assertEquals(GtfsRealtime.VehiclePosition.VehicleStopStatus.IN_TRANSIT_TO, stopStatus7.stopStatus);
        assertEquals("3", stopStatus7.stopId);

        assertNull(calculator.getStopStatus(generateHfpData(Hfp.Topic.EventType.VP, "EOL", null)));
    }

    @Test
    public void testStopStatusNullIfNextStopIsEmpty() {
        StopStatusProcessor processor = new StopStatusProcessor();

        assertNull(processor.getStopStatus(generateHfpData(Hfp.Topic.EventType.VP, "", null)));
    }

    @Test
    public void testStopStatusWithMetro() {
        StopStatusProcessor processor = new StopStatusProcessor();

        StopStatusProcessor.StopStatus stoppedAtStop1 = processor.getStopStatus(generateHfpData(Hfp.Topic.EventType.VP, Hfp.Topic.TransportMode.metro, "1", "1"));
        assertEquals(GtfsRealtime.VehiclePosition.VehicleStopStatus.STOPPED_AT, stoppedAtStop1.stopStatus);
        assertEquals("1", stoppedAtStop1.stopId);

        StopStatusProcessor.StopStatus inTransitToStop2 = processor.getStopStatus(generateHfpData(Hfp.Topic.EventType.VP, Hfp.Topic.TransportMode.metro, "2", null));
        assertEquals(GtfsRealtime.VehiclePosition.VehicleStopStatus.IN_TRANSIT_TO, inTransitToStop2.stopStatus);
        assertEquals("2", inTransitToStop2.stopId);
    }
}
