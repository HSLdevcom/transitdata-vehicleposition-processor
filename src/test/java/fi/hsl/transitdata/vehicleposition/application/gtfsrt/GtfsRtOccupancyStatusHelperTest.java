package fi.hsl.transitdata.vehicleposition.application.gtfsrt;

import com.google.transit.realtime.GtfsRealtime;
import fi.hsl.common.hfp.proto.Hfp;
import fi.hsl.common.passengercount.proto.PassengerCount;
import org.junit.Before;
import org.junit.Test;

import java.util.Optional;
import java.util.TreeMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class GtfsRtOccupancyStatusHelperTest {
    private GtfsRtOccupancyStatusHelper gtfsRtOccupancyStatusHelper;

    @Before
    public void setup() {
        TreeMap<Integer, GtfsRealtime.VehiclePosition.OccupancyStatus> occuToOccupancyStatus = new TreeMap<>();
        occuToOccupancyStatus.put(0, GtfsRealtime.VehiclePosition.OccupancyStatus.EMPTY);
        occuToOccupancyStatus.put(5, GtfsRealtime.VehiclePosition.OccupancyStatus.MANY_SEATS_AVAILABLE);
        occuToOccupancyStatus.put(20, GtfsRealtime.VehiclePosition.OccupancyStatus.FEW_SEATS_AVAILABLE);
        occuToOccupancyStatus.put(50, GtfsRealtime.VehiclePosition.OccupancyStatus.STANDING_ROOM_ONLY);
        occuToOccupancyStatus.put(70, GtfsRealtime.VehiclePosition.OccupancyStatus.CRUSHED_STANDING_ROOM_ONLY);
        occuToOccupancyStatus.put(90, GtfsRealtime.VehiclePosition.OccupancyStatus.FULL);

        TreeMap<Double, GtfsRealtime.VehiclePosition.OccupancyStatus> loadRatioToOccypancyStatus = new TreeMap<>();
        loadRatioToOccypancyStatus.put(0.0, GtfsRealtime.VehiclePosition.OccupancyStatus.EMPTY);
        loadRatioToOccypancyStatus.put(0.05, GtfsRealtime.VehiclePosition.OccupancyStatus.MANY_SEATS_AVAILABLE);
        loadRatioToOccypancyStatus.put(0.2, GtfsRealtime.VehiclePosition.OccupancyStatus.FEW_SEATS_AVAILABLE);
        loadRatioToOccypancyStatus.put(0.5, GtfsRealtime.VehiclePosition.OccupancyStatus.STANDING_ROOM_ONLY);
        loadRatioToOccypancyStatus.put(0.7, GtfsRealtime.VehiclePosition.OccupancyStatus.CRUSHED_STANDING_ROOM_ONLY);
        loadRatioToOccypancyStatus.put(0.9, GtfsRealtime.VehiclePosition.OccupancyStatus.FULL);

        gtfsRtOccupancyStatusHelper = new GtfsRtOccupancyStatusHelper(occuToOccupancyStatus, loadRatioToOccypancyStatus);
    }

    @Test
    public void testHfpOccupancyStatus() {
        Optional<GtfsRealtime.VehiclePosition.OccupancyStatus> occuStatus =
                gtfsRtOccupancyStatusHelper.getOccupancyStatus(Hfp.Payload.newBuilder().
                        setSchemaVersion(1).setTsi(0).setTst("").setOccu(55).build(), null);

        assertTrue(occuStatus.isPresent());
        assertEquals(GtfsRealtime.VehiclePosition.OccupancyStatus.STANDING_ROOM_ONLY, occuStatus.get());
    }
    
    @Test
    public void testHfpOccupancyStatusEmpty() {
        Optional<GtfsRealtime.VehiclePosition.OccupancyStatus> occuStatus =
                gtfsRtOccupancyStatusHelper.getOccupancyStatus(Hfp.Payload.newBuilder().
                        setSchemaVersion(1).setTsi(0).setTst("").setOccu(0).build(), null);
        
        assertTrue(occuStatus.isPresent());
        assertEquals(GtfsRealtime.VehiclePosition.OccupancyStatus.EMPTY, occuStatus.get());
    }
    
    @Test
    public void testHfpOccupancyStatusNegative() {
        Optional<GtfsRealtime.VehiclePosition.OccupancyStatus> occuStatus =
                gtfsRtOccupancyStatusHelper.getOccupancyStatus(Hfp.Payload.newBuilder().
                        setSchemaVersion(1).setTsi(0).setTst("").setOccu(-1).build(), null);
        
        assertTrue(occuStatus.isPresent());
        assertEquals(GtfsRealtime.VehiclePosition.OccupancyStatus.EMPTY, occuStatus.get());
    }
    
    @Test
    public void testPassengerCountOccupancyStatus() {
        Optional<GtfsRealtime.VehiclePosition.OccupancyStatus> occuStatus =
                gtfsRtOccupancyStatusHelper.getOccupancyStatus(
                        Hfp.Payload.newBuilder().
                                setSchemaVersion(1).setTsi(0).setTst("").setOccu(0).build(),
                        PassengerCount.Payload.newBuilder().setVehicleCounts(PassengerCount.VehicleCounts.newBuilder()
                                .setCountQuality("").setVehicleLoad(10).setVehicleLoadRatio(0.55).build()).build());
        
        assertTrue(occuStatus.isPresent());
        assertEquals(GtfsRealtime.VehiclePosition.OccupancyStatus.STANDING_ROOM_ONLY, occuStatus.get());
    }
    
    @Test
    public void testPassengerCountOccupancyStatusEmpty() {
        Optional<GtfsRealtime.VehiclePosition.OccupancyStatus> occuStatus =
                gtfsRtOccupancyStatusHelper.getOccupancyStatus(
                        Hfp.Payload.newBuilder().
                                setSchemaVersion(1).setTsi(0).setTst("").setOccu(0).build(),
                        PassengerCount.Payload.newBuilder().setVehicleCounts(PassengerCount.VehicleCounts.newBuilder()
                                .setCountQuality("").setVehicleLoad(0).setVehicleLoadRatio(0.0).build()).build());
    
        assertTrue(occuStatus.isPresent());
        assertEquals(GtfsRealtime.VehiclePosition.OccupancyStatus.EMPTY, occuStatus.get());
    }
    
    @Test
    public void testPassengerCountOccupancyStatusNegative() {
        Optional<GtfsRealtime.VehiclePosition.OccupancyStatus> occuStatus =
                gtfsRtOccupancyStatusHelper.getOccupancyStatus(
                        Hfp.Payload.newBuilder().
                                setSchemaVersion(1).setTsi(0).setTst("").setOccu(0).build(),
                        PassengerCount.Payload.newBuilder().setVehicleCounts(PassengerCount.VehicleCounts.newBuilder()
                                .setCountQuality("").setVehicleLoad(0).setVehicleLoadRatio(-1.0).build()).build());
        
        assertTrue(occuStatus.isPresent());
        assertEquals(GtfsRealtime.VehiclePosition.OccupancyStatus.EMPTY, occuStatus.get());
    }
}
