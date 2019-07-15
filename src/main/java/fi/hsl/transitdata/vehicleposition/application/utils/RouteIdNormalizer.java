package fi.hsl.transitdata.vehicleposition.application.utils;

public class RouteIdNormalizer {
    private RouteIdNormalizer() {}

    /**
     * Normalizes route id variants, e.g. '1008 3' -> '1008' and '3001Z3' -> '3001Z'
     * @param routeId Route Id
     * @return Normalized route id
     */
    public static String normalizeRouteId(String routeId) {
        if (routeId.length() < 4) {
            throw new IllegalArgumentException("Route ID must be at least 4 characters");
        } else if (routeId.length() <= 5) {
            return routeId;
        } else {
            if (Character.isAlphabetic(routeId.charAt(4)) && !Character.isAlphabetic(routeId.charAt(5))) {
                return routeId.substring(0, 5);
            } else if (Character.isSpaceChar(routeId.charAt(4))) {
                return routeId.substring(0, 4);
            } else {
                return routeId;
            }
        }
    }
}
