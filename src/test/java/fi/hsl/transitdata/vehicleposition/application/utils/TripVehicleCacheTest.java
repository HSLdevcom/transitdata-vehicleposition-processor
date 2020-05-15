package fi.hsl.transitdata.vehicleposition.application.utils;

import fi.hsl.transitdata.vehicleposition.application.utils.TripVehicleCache;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class TripVehicleCacheTest {
    private TripVehicleCache tripVehicleCache;

    @Before
    public void setup() {
        tripVehicleCache = new TripVehicleCache();
    }

    @Test
    public void testCanRegisterVehicleForTrip() {
        assertTrue(tripVehicleCache.registerVehicleForTrip("10/1515", "2550", "2019-08-17", "09:16", "1"));
    }

    @Test
    public void testOnlyOneVehicleCanBeRegisteredForOneTrip() {
        assertTrue(tripVehicleCache.registerVehicleForTrip("10/1515", "2550", "2019-08-17", "09:16", "1"));
        assertFalse(tripVehicleCache.registerVehicleForTrip("10/1516", "2550", "2019-08-17", "09:16", "1"));
    }

    @Test
    public void testSameVehicleCanBeRegisteredAgain() {
        assertTrue(tripVehicleCache.registerVehicleForTrip("10/1515", "2550", "2019-08-17", "09:16", "1"));
        assertTrue(tripVehicleCache.registerVehicleForTrip("10/1515", "2550", "2019-08-17", "09:16", "1"));
    }
}
