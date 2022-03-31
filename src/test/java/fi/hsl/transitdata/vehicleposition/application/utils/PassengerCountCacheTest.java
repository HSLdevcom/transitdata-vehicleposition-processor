package fi.hsl.transitdata.vehicleposition.application.utils;

import fi.hsl.common.passengercount.proto.PassengerCount;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class PassengerCountCacheTest {
    private PassengerCountCache passengerCountCache;

    @Before
    public void setup() {
        passengerCountCache = new PassengerCountCache();
    }

    @Test
    public void testPassengerCountCache() {
        passengerCountCache.updatePassengerCount("1/1", "1", "2021-01-01", "12:00", "1", PassengerCount.Payload.newBuilder()
                        .setDesi("1")
                        .setDir("1")
                        .setJrn(1)
                        .setLat(0)
                        .setLine(1)
                        .setLoc("GPS")
                        .setLong(0)
                        .setOday("2021-01-01")
                        .setOdo(0)
                        .setOper(1)
                        .setRoute("1")
                        .setStart("12:00")
                        .setStop(1)
                        .setTsi(0)
                        .setTst(0)
                        .setVeh(1)
                        .setVehicleCounts(PassengerCount.VehicleCounts.newBuilder()
                                .setCountQuality("")
                                .setVehicleLoad(10)
                                .setVehicleLoadRatio(0.25)
                                .build())
                .build());

        assertNotNull(passengerCountCache.getPassengerCount("1/1", "1", "2021-01-01", "12:00", "1"));
        assertEquals(0.25, passengerCountCache.getPassengerCount("1/1", "1", "2021-01-01", "12:00", "1").getVehicleCounts().getVehicleLoadRatio(), 0.00001);
    }
}
