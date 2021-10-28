package fi.hsl.transitdata.vehicleposition.application.utils;

import fi.hsl.common.passengercount.proto.PassengerCount;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class PassengerCountCache {
    //Max size for cache to prune data for trips that have been completed
    private static final int MAX_SIZE = 5000;
    //Remove vehicle ids for trips older than this
    private static final int MAX_AGE_HOURS = 3;

    private Map<VehicleIdAndTrip, PassengerCount.Payload> vehicleIdAndTripToPassengerCount = new HashMap<>(1000);

    public void updatePassengerCount(String uniqueVehicleId, String routeId, String operatingDay, String startTime, String directionId, PassengerCount.Payload passengerCount) {
        if (vehicleIdAndTripToPassengerCount.size() >= MAX_SIZE) {
            vehicleIdAndTripToPassengerCount.keySet().removeIf(vehicleIdAndTrip ->
                    System.nanoTime() - vehicleIdAndTrip.createTime > TimeUnit.NANOSECONDS.convert(MAX_AGE_HOURS, TimeUnit.HOURS)
            );
        }

        vehicleIdAndTripToPassengerCount.put(new VehicleIdAndTrip(uniqueVehicleId, routeId, operatingDay, startTime, directionId), passengerCount);
    }

    public PassengerCount.Payload getPassengerCount(String uniqueVehicleId, String routeId, String operatingDay, String startTime, String directionId) {
        return vehicleIdAndTripToPassengerCount.get(new VehicleIdAndTrip(uniqueVehicleId, routeId, operatingDay, startTime, directionId));
    }

    private static class VehicleIdAndTrip {
        public final String uniqueVehicleId;
        public final String routeId;
        public final String operatingDay;
        public final String startTime;
        public final String directionId;

        public final long createTime = System.nanoTime();

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
