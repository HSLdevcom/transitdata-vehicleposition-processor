package fi.hsl.transitdata.vehicleposition.application.gtfsrt;

import com.google.transit.realtime.GtfsRealtime;
import fi.hsl.common.hfp.proto.Hfp;
import fi.hsl.transitdata.vehicleposition.application.StopStatusProcessor;
import fi.hsl.transitdata.vehicleposition.application.utils.RouteIdNormalizer;

import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.SortedMap;
import java.util.function.Function;

import static fi.hsl.transitdata.vehicleposition.application.utils.TimeUtils.getStartTime;

public class GtfsRtGenerator {
    private GtfsRtGenerator() {}

    public static Optional<GtfsRealtime.VehiclePosition> generateVehiclePosition(Hfp.Data hfpData, StopStatusProcessor.StopStatus stopStatus, NavigableMap<Integer, GtfsRealtime.VehiclePosition.OccupancyStatus> occupancyStatusMap) {
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

        getOccupancyStatus(hfpData.getPayload(), occupancyStatusMap).ifPresent(vp::setOccupancyStatus);

        return Optional.of(vp.build());
    }

    private static Optional<GtfsRealtime.VehiclePosition.OccupancyStatus> getOccupancyStatus(Hfp.Payload payload, NavigableMap<Integer, GtfsRealtime.VehiclePosition.OccupancyStatus> occupancyStatusMap) {
        if (!payload.hasOccu()) {
            return Optional.empty();
        } else {
            return Optional.ofNullable(occupancyStatusMap.lowerEntry(payload.getOccu())).map(Map.Entry::getValue);
        }
    }
}
