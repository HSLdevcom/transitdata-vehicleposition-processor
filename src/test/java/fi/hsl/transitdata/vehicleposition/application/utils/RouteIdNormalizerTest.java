package fi.hsl.transitdata.vehicleposition.application.utils;

import fi.hsl.transitdata.vehicleposition.application.utils.RouteIdNormalizer;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class RouteIdNormalizerTest {
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidIdThrowsException() {
        RouteIdNormalizer.normalizeRouteId("100");
    }

    @Test
    public void testNormalIdReturnsItself_1() {
        String routeId = "2550";

        assertEquals(routeId, RouteIdNormalizer.normalizeRouteId(routeId));
    }

    @Test
    public void testNormalIdReturnsItself_2() {
        String routeId = "3001Z";

        assertEquals(routeId, RouteIdNormalizer.normalizeRouteId(routeId));
    }

    @Test
    public void testNormalizeTramIdVariant() {
        assertEquals("1008", RouteIdNormalizer.normalizeRouteId("1008 3"));
    }

    @Test
    public void testNormalizeTrainIdVariant() {
        assertEquals("3001Z", RouteIdNormalizer.normalizeRouteId("3001Z3"));
    }

    @Test
    public void testNormalizeBusIdVariant() {
        assertEquals("9787A", RouteIdNormalizer.normalizeRouteId("9787A3"));
    }

    @Test
    public void testRouteIdWithTwoCharactersReturnsItself() {
        assertEquals("2348BK", RouteIdNormalizer.normalizeRouteId("2348BK"));
    }
}
