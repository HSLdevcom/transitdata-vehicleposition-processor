package fi.hsl.transitdata.vehicleposition.application.utils;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Scheduler;
import fi.hsl.common.passengercount.proto.PassengerCount;

import java.time.Duration;
import java.util.Objects;

public class PassengerCountCache {
    //Remove old data after 5 minutes (passenger count data that is too old is not relevant, because we are interested in the current load of the vehicle)
    //TODO: this should be configurable
    private static final Duration MAX_AGE = Duration.ofMinutes(5);

    private final Cache<VehicleIdAndTrip, PassengerCount.Data> passengerCountCache = Caffeine.newBuilder()
            .expireAfterWrite(MAX_AGE)
            .scheduler(Scheduler.systemScheduler())
            .build();

    public void updatePassengerCount(String uniqueVehicleId, String routeId, String operatingDay, String startTime, String directionId, PassengerCount.Data passengerCount) {
        passengerCountCache.put(new VehicleIdAndTrip(uniqueVehicleId, routeId, operatingDay, startTime, directionId), passengerCount);
    }

    public PassengerCount.Data getPassengerCount(String uniqueVehicleId, String routeId, String operatingDay, String startTime, String directionId) {
        return passengerCountCache.getIfPresent(new VehicleIdAndTrip(uniqueVehicleId, routeId, operatingDay, startTime, directionId));
    }

    private static class VehicleIdAndTrip {
        public final String uniqueVehicleId;
        public final String routeId;
        public final String operatingDay;
        public final String startTime;
        public final String directionId;

        private VehicleIdAndTrip(String uniqueVehicleId, String routeId, String operatingDay, String startTime, String directionId) {
            this.uniqueVehicleId = uniqueVehicleId;
            this.routeId = routeId;
            this.operatingDay = operatingDay;
            this.startTime = startTime;
            this.directionId = directionId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            VehicleIdAndTrip that = (VehicleIdAndTrip) o;
            return Objects.equals(uniqueVehicleId, that.uniqueVehicleId) && Objects.equals(routeId, that.routeId) && Objects.equals(operatingDay, that.operatingDay) && Objects.equals(startTime, that.startTime) && Objects.equals(directionId, that.directionId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(uniqueVehicleId, routeId, operatingDay, startTime, directionId);
        }
    }
}
