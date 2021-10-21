package fi.hsl.transitdata.vehicleposition.application.gtfsrt;

import com.google.transit.realtime.GtfsRealtime;
import fi.hsl.common.hfp.proto.Hfp;
import fi.hsl.transitdata.vehicleposition.application.StopStatusProcessor;
import fi.hsl.transitdata.vehicleposition.application.gtfsrt.GtfsRtGenerator;
import org.junit.Test;

import java.util.Collections;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;

import static com.google.transit.realtime.GtfsRealtime.VehiclePosition.VehicleStopStatus.IN_TRANSIT_TO;
import static org.junit.Assert.*;

public class GtfsRtGeneratorTest {
    @Test
    public void testNoVehiclePositionGeneratedIfNoLocation() {
        Hfp.Data data = Hfp.Data.newBuilder()
                            .setSchemaVersion(1)
                            .setPayload(Hfp.Payload.newBuilder()
                                    .setSchemaVersion(1)
                                    .setTst("")
                                    .setTsi(0))
                            .build();

        assertFalse(GtfsRtGenerator.generateVehiclePosition(data, new StopStatusProcessor.StopStatus("1", IN_TRANSIT_TO), Optional.empty()).isPresent());
    }

    @Test
    public void testGtfsRtVehiclePositionHasCorrectValues() {
        Hfp.Data data = Hfp.Data.newBuilder()
                            .setSchemaVersion(1)
                            .setTopic(Hfp.Topic.newBuilder()
                                .setSchemaVersion(1)
                                .setReceivedAt(0)
                                .setTopicPrefix("hfp")
                                .setTopicVersion("v2")
                                .setJourneyType(Hfp.Topic.JourneyType.journey)
                                .setTemporalType(Hfp.Topic.TemporalType.ongoing)
                                .setOperatorId(1)
                                .setVehicleNumber(1)
                                .setUniqueVehicleId("1/1")
                                .setDirectionId(1)
                                .setRouteId("1999"))
                            .setPayload(Hfp.Payload.newBuilder()
                                .setSchemaVersion(1)
                                .setLat(60)
                                .setLong(25)
                                .setSpd(10)
                                .setHdg(60)
                                .setOdo(10000)
                                .setTsi(1562655000)
                                .setTst("2019-07-09T06:50:00.000Z")
                                .setOday("2019-07-09")
                                .setStart("09:30")
                                .setOccu(100))
                            .build();

        GtfsRealtime.VehiclePosition gtfsRtVp = GtfsRtGenerator.generateVehiclePosition(data, new StopStatusProcessor.StopStatus("1", IN_TRANSIT_TO), Optional.empty()).get();

        assertEquals(1562655000, gtfsRtVp.getTimestamp());
        assertEquals(60, gtfsRtVp.getPosition().getLatitude(), 0.001);
        assertEquals(25, gtfsRtVp.getPosition().getLongitude(), 0.001);
        assertEquals(10, gtfsRtVp.getPosition().getSpeed(), 0.001);
        assertEquals(60, gtfsRtVp.getPosition().getBearing(), 0.001);
        assertEquals(10000, gtfsRtVp.getPosition().getOdometer(), 0.001);
        assertEquals("1/1", gtfsRtVp.getVehicle().getId());
        assertEquals("20190709", gtfsRtVp.getTrip().getStartDate());
        assertEquals("09:30:00", gtfsRtVp.getTrip().getStartTime());
        assertEquals(0, gtfsRtVp.getTrip().getDirectionId());
        assertEquals("1999", gtfsRtVp.getTrip().getRouteId());
    }

    @Test
    public void testGtfsRtVehiclePositionHasCorrectValuesWithLabel() {
        Hfp.Data data = Hfp.Data.newBuilder()
                .setSchemaVersion(1)
                .setTopic(Hfp.Topic.newBuilder()
                        .setSchemaVersion(1)
                        .setReceivedAt(0)
                        .setTopicPrefix("hfp")
                        .setTopicVersion("v2")
                        .setJourneyType(Hfp.Topic.JourneyType.journey)
                        .setTemporalType(Hfp.Topic.TemporalType.ongoing)
                        .setOperatorId(1)
                        .setVehicleNumber(1)
                        .setUniqueVehicleId("1/1")
                        .setDirectionId(1)
                        .setRouteId("1999"))
                .setPayload(Hfp.Payload.newBuilder()
                        .setSchemaVersion(1)
                        .setLat(60)
                        .setLong(25)
                        .setSpd(10)
                        .setHdg(60)
                        .setOdo(10000)
                        .setTsi(1562655000)
                        .setTst("2019-07-09T06:50:00.000Z")
                        .setOday("2019-07-09")
                        .setStart("09:30")
                        .setLabel("SUOMENLINNA II")
                        .setOccu(100))
                .build();

        GtfsRealtime.VehiclePosition gtfsRtVp = GtfsRtGenerator.generateVehiclePosition(data, new StopStatusProcessor.StopStatus("1", IN_TRANSIT_TO), Optional.empty()).get();

        assertEquals(1562655000, gtfsRtVp.getTimestamp());
        assertEquals(60, gtfsRtVp.getPosition().getLatitude(), 0.001);
        assertEquals(25, gtfsRtVp.getPosition().getLongitude(), 0.001);
        assertEquals(10, gtfsRtVp.getPosition().getSpeed(), 0.001);
        assertEquals(60, gtfsRtVp.getPosition().getBearing(), 0.001);
        assertEquals(10000, gtfsRtVp.getPosition().getOdometer(), 0.001);
        assertEquals("1/1", gtfsRtVp.getVehicle().getId());
        assertEquals("20190709", gtfsRtVp.getTrip().getStartDate());
        assertEquals("09:30:00", gtfsRtVp.getTrip().getStartTime());
        assertEquals(0, gtfsRtVp.getTrip().getDirectionId());
        assertEquals("1999", gtfsRtVp.getTrip().getRouteId());
        assertEquals("SUOMENLINNA II", gtfsRtVp.getVehicle().getLabel());
    }
}
