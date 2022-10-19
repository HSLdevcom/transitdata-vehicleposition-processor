package fi.hsl.transitdata.vehicleposition.application.utils;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Scheduler;

import java.time.Duration;
import java.util.Objects;

public class TripVehicleCache {
    //Remove trip registrations older than this (i.e. after this time another vehicle could take the same trip)
    private static final Duration MAX_AGE = Duration.ofHours(3);

    private final Cache<TripDescriptor, String> tripRegistrationCache = Caffeine.newBuilder()
            .expireAfterWrite(MAX_AGE)
            .scheduler(Scheduler.systemScheduler())
            .build();

    /**
     * Registers the vehicle for a trip. Only one vehicle can be registered for a single trip.
     * @param vehicleId
     * @param routeId
     * @param operatingDay
     * @param startTime
     * @param directionId
     * @return true if the vehicle was registered for the trip. false if some other vehicle was already registered.
     */
    public boolean registerVehicleForTrip(String vehicleId, String routeId, String operatingDay, String startTime, String directionId) {
        String registeredVehicleId = tripRegistrationCache.asMap().putIfAbsent(new TripDescriptor(routeId, operatingDay, startTime, directionId), vehicleId);
        return registeredVehicleId == null || vehicleId.equals(registeredVehicleId);
    }

    private static class TripDescriptor {
        public final String routeId;
        public final String operatingDay;
        public final String startTime;
        public final String directionId;

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
