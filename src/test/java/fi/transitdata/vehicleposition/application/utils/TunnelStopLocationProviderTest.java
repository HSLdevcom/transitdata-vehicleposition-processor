package fi.transitdata.vehicleposition.application.utils;

import fi.hsl.transitdata.vehicleposition.application.utils.TunnelStopLocationProvider;
import org.junit.Before;
import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.*;

public class TunnelStopLocationProviderTest {
    private TunnelStopLocationProvider tunnelStopLocationProvider;

    @Before
    public void setup() {
        tunnelStopLocationProvider = new TunnelStopLocationProvider();
    }

    @Test
    public void testNoLocationForUnknownStop() throws Exception {
        assertFalse(tunnelStopLocationProvider.getStopLocation("invalid_stop_id").isPresent());
    }

    @Test
    public void testCanFindLocationForLentoasema() throws Exception {
        Optional<double[]> maybeLocation = tunnelStopLocationProvider.getStopLocation("4530501");

        assertTrue(maybeLocation.isPresent());

        double[] location = maybeLocation.get();

        assertEquals(60.315759, location[0], 0.001);
        assertEquals(24.968607, location[1], 0.001);
    }
}
