package fi.hsl.transitdata.vehicleposition.application;

import com.google.transit.realtime.GtfsRealtime;
import fi.hsl.transitdata.vehicleposition.application.VehiclePositionHandler;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

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
}
