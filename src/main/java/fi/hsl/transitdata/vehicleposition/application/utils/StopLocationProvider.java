package fi.hsl.transitdata.vehicleposition.application.utils;

import java.util.Optional;

public interface StopLocationProvider {
    Optional<double[]> getStopLocation(String stopId) throws Exception;
}
