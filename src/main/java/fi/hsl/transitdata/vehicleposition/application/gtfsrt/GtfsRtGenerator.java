package fi.hsl.transitdata.vehicleposition.application.gtfsrt;

import com.google.transit.realtime.GtfsRealtime;
import fi.hsl.common.hfp.proto.Hfp;
import fi.hsl.transitdata.vehicleposition.application.StopStatusProcessor;
import fi.hsl.transitdata.vehicleposition.application.utils.RouteIdNormalizer;

import java.util.Optional;

import static fi.hsl.transitdata.vehicleposition.application.utils.TimeUtils.getStartTime;

public class GtfsRtGenerator {
    private GtfsRtGenerator() {}

    public static Optional<GtfsRealtime.VehiclePosition> generateVehiclePosition(Hfp.Data hfpData, StopStatusProcessor.StopStatus stopStatus) {
        //Ignore messages where the vehicle has no location
        if (!hfpData.getPayload().hasLat() || !hfpData.getPayload().hasLong()) {
            return Optional.empty();
        }

        GtfsRealtime.VehiclePosition.Builder vp = GtfsRealtime.VehiclePosition.newBuilder();

        vp.setTimestamp(hfpData.getPayload().getTsi());
        //Do not set stop status if the final stop has been reached or the next stop is outside HSL area
        if (stopStatus != null) {
            vp.setCurrentStatus(stopStatus.stopStatus);
            vp.setStopId(stopStatus.stopId);
        }

        vp.setPosition(GtfsRealtime.Position.newBuilder()
                .setLatitude((float) hfpData.getPayload().getLat())
                .setLongitude((float) hfpData.getPayload().getLong())
                .setSpeed((float) hfpData.getPayload().getSpd())
                .setBearing(hfpData.getPayload().getHdg())
                .setOdometer(hfpData.getPayload().getOdo()));

        GtfsRealtime.VehicleDescriptor.Builder vehicleDescriptor = GtfsRealtime.VehicleDescriptor.newBuilder()
                .setId(hfpData.getTopic().getUniqueVehicleId());
        if (hfpData.getPayload().hasLabel()) {
            vehicleDescriptor.setLabel(hfpData.getPayload().getLabel());
        }

        vp.setVehicle(vehicleDescriptor);

        String startTime = getStartTime(hfpData);

        vp.setTrip(GtfsRealtime.TripDescriptor.newBuilder()
                //TODO: figure out how to set schedule relationship correctly
                //.setScheduleRelationship(GtfsRealtime.TripDescriptor.ScheduleRelationship.SCHEDULED)
                .setDirectionId(hfpData.getTopic().getDirectionId() - 1)
                .setRouteId(RouteIdNormalizer.normalizeRouteId(hfpData.getTopic().getRouteId()))
                .setStartDate(hfpData.getPayload().getOday().replaceAll("-", ""))
                .setStartTime(startTime));

        getOccupancyStatus(hfpData.getPayload()).ifPresent(vp::setOccupancyStatus);

        return Optional.of(vp.build());
    }

    private static Optional<GtfsRealtime.VehiclePosition.OccupancyStatus> getOccupancyStatus(Hfp.Payload payload) {
        //TODO: these values should be configurable
        if (!payload.hasOccu() || payload.getOccu() == 0) {
            return Optional.empty();
        } else if (payload.getOccu() <= 5) {
            return Optional.of(GtfsRealtime.VehiclePosition.OccupancyStatus.EMPTY);
        } else if (payload.getOccu() <= 20) {
            return Optional.of(GtfsRealtime.VehiclePosition.OccupancyStatus.MANY_SEATS_AVAILABLE);
        } else if (payload.getOccu() <= 50) {
            return Optional.of(GtfsRealtime.VehiclePosition.OccupancyStatus.FEW_SEATS_AVAILABLE);
        } else if (payload.getOccu() <= 70) {
            return Optional.of(GtfsRealtime.VehiclePosition.OccupancyStatus.STANDING_ROOM_ONLY);
        } else if (payload.getOccu() <= 90) {
            return Optional.of(GtfsRealtime.VehiclePosition.OccupancyStatus.CRUSHED_STANDING_ROOM_ONLY);
        } else {
            return Optional.of(GtfsRealtime.VehiclePosition.OccupancyStatus.FULL);
        }
    }
}
