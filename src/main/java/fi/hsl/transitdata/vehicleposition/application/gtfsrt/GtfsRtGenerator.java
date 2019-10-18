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
        GtfsRealtime.VehiclePosition.Builder vp = GtfsRealtime.VehiclePosition.newBuilder();

        vp.setTimestamp(hfpData.getPayload().getTsi());
        //Do not set stop status if the final stop has been reached or the next stop is outside HSL area
        if (stopStatus != null) {
            vp.setCurrentStatus(stopStatus.stopStatus);
            vp.setStopId(stopStatus.stopId);
        }

        //If there is no GPS fix, do not add position data to GTFS-RT VP message.
        //Google seems to use VP messages to calculate arrival/departure time predictions, so we want to send them vehicle's stop status even if its position is unknown
        if (!hfpData.getPayload().hasLat() || !hfpData.getPayload().hasLong()) {
            vp.setPosition(GtfsRealtime.Position.newBuilder()
                    .setLatitude((float) hfpData.getPayload().getLat())
                    .setLongitude((float) hfpData.getPayload().getLong())
                    .setSpeed((float) hfpData.getPayload().getSpd())
                    .setBearing(hfpData.getPayload().getHdg())
                    .setOdometer(hfpData.getPayload().getOdo()));
        }

        vp.setVehicle(GtfsRealtime.VehicleDescriptor.newBuilder()
                .setId(hfpData.getTopic().getUniqueVehicleId()));

        String startTime = getStartTime(hfpData);

        vp.setTrip(GtfsRealtime.TripDescriptor.newBuilder()
                //TODO: figure out how to set schedule relationship correctly
                //.setScheduleRelationship(GtfsRealtime.TripDescriptor.ScheduleRelationship.SCHEDULED)
                .setDirectionId(hfpData.getTopic().getDirectionId() - 1)
                .setRouteId(RouteIdNormalizer.normalizeRouteId(hfpData.getTopic().getRouteId()))
                .setStartDate(hfpData.getPayload().getOday().replaceAll("-", ""))
                .setStartTime(startTime));

        if (hfpData.getPayload().getOccu() == 100) {
            vp.setOccupancyStatus(GtfsRealtime.VehiclePosition.OccupancyStatus.FULL);
        }

        return Optional.of(vp.build());
    }
}
