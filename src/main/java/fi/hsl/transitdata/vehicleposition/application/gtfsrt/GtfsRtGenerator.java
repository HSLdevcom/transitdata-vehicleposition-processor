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
        //Ignore messages where the vehicle has no location or it has no stop status after reaching the final stop
        if (!hfpData.getPayload().hasLat() || !hfpData.getPayload().hasLong() || stopStatus == null) {
            return Optional.empty();
        }

        GtfsRealtime.VehiclePosition.Builder vp = GtfsRealtime.VehiclePosition.newBuilder();

        vp.setTimestamp(hfpData.getPayload().getTsi());
        vp.setCurrentStatus(stopStatus.stopStatus);
        vp.setStopId(stopStatus.stopId);

        vp.setPosition(GtfsRealtime.Position.newBuilder()
                .setLatitude((float) hfpData.getPayload().getLat())
                .setLongitude((float) hfpData.getPayload().getLong())
                .setSpeed((float) hfpData.getPayload().getSpd())
                .setBearing(hfpData.getPayload().getHdg())
                .setOdometer(hfpData.getPayload().getOdo()));

        vp.setVehicle(GtfsRealtime.VehicleDescriptor.newBuilder()
                .setId(hfpData.getTopic().getUniqueVehicleId()));

        String startTime = getStartTime(hfpData);

        vp.setTrip(GtfsRealtime.TripDescriptor.newBuilder()
                .setScheduleRelationship(GtfsRealtime.TripDescriptor.ScheduleRelationship.SCHEDULED)
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
