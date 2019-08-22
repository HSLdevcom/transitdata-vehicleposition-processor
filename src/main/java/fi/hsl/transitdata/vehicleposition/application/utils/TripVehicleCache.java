package fi.hsl.transitdata.vehicleposition.application.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class TripVehicleCache {
    //Max size for cache to prune data for trips that have been completed
    private static final int MAX_SIZE = 5000;
    //Remove vehicle ids for trips older than this
    private static final int MAX_AGE_HOURS = 3;

    private Map<TripDescriptor, String> tripToVehicleId = new HashMap<>(1000);

    /**
     * Registers the vehicle for a trip. Only one vehicle can be registered for a single trip.
     * @param vehicleId
     * @param routeId
     * @param operatingDay
     * @param startTime
     * @param directionId
     * @return true if the vehicle was registed for the trip. false if some other vehicle was already registered.
     */
    public boolean registerVehicleForTrip(String vehicleId, String routeId, String operatingDay, String startTime, String directionId) {
        if (tripToVehicleId.size() >= MAX_SIZE) {
            tripToVehicleId.keySet().removeIf(tripDescriptor -> System.nanoTime() - tripDescriptor.createTime > TimeUnit.NANOSECONDS.convert(MAX_AGE_HOURS, TimeUnit.HOURS));
        }

        String registeredVehicleId = tripToVehicleId.putIfAbsent(new TripDescriptor(routeId, operatingDay, startTime, directionId), vehicleId);
        return registeredVehicleId == null || vehicleId.equals(registeredVehicleId);
    }

    private static class TripDescriptor {
        public final String routeId;
        public final String operatingDay;
        public final String startTime;
        public final String directionId;

        public final long createTime = System.nanoTime();

        public TripDescriptor(String routeId, String operatingDay, String startTime, String directionId) {
            this.routeId = routeId;
            this.operatingDay = operatingDay;
            this.startTime = startTime;
            this.directionId = directionId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TripDescriptor that = (TripDescriptor) o;
            return Objects.equals(routeId, that.routeId) &&
                    Objects.equals(operatingDay, that.operatingDay) &&
                    Objects.equals(startTime, that.startTime) &&
                    Objects.equals(directionId, that.directionId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(routeId, operatingDay, startTime, directionId);
        }
    }
}
