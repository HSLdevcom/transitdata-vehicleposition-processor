package fi.hsl.transitdata.vehicleposition.application.utils;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigObject;
import fi.hsl.common.config.ConfigParser;

import java.util.*;

public class TunnelStopLocationProvider implements StopLocationProvider {
    private static final Map<String, double[]> STOP_LOCATIONS;

    static {
        Map<String, double[]> stops = new HashMap<>();

        final Config tunnelStops = ConfigParser.createConfig("tunnel_stops.conf");
        tunnelStops.getObjectList("tunnelStops").stream().map(ConfigObject::toConfig).forEach(tunnelStopConfig -> {
            final String stopId = tunnelStopConfig.getString("id");
            final Double[] location = tunnelStopConfig.getDoubleList("location").toArray(new Double[2]);

            stops.put(stopId, Arrays.stream(location).mapToDouble(d -> d).toArray());
        });

        STOP_LOCATIONS = Collections.unmodifiableMap(stops);
    }

    @Override
    public Optional<double[]> getStopLocation(String stopId) throws Exception {
        return Optional.ofNullable(STOP_LOCATIONS.get(stopId));
    }
}
