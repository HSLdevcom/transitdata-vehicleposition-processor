package fi.hsl.transitdata.vehicleposition.application.gtfsrt;

import com.google.transit.realtime.GtfsRealtime;
import fi.hsl.common.hfp.proto.Hfp;
import fi.hsl.transitdata.vehicleposition.application.StopStatusProcessor;
import fi.hsl.transitdata.vehicleposition.application.utils.RouteIdNormalizer;
import fi.hsl.transitdata.vehicleposition.application.utils.StopLocationProvider;
import fi.hsl.transitdata.vehicleposition.application.utils.TunnelStopLocationProvider;

import java.util.Optional;

import static fi.hsl.transitdata.vehicleposition.application.utils.TimeUtils.getStartTime;

public class GtfsRtGenerator {
    private static final StopLocationProvider STOP_LOCATION_PROVIDER = new TunnelStopLocationProvider();

    private GtfsRtGenerator() {}

    public static Optional<GtfsRealtime.VehiclePosition> generateVehiclePosition(Hfp.Data hfpData, StopStatusProcessor.StopStatus stopStatus) {
        //If the vehicle is stopped at a stop, we can use that location of that stop for vehicle position
        //This is required to display location of trains in Kehärata tunnel
        Optional<double[]> maybeStopLocation;
        try {
            maybeStopLocation = stopStatus.stopStatus == GtfsRealtime.VehiclePosition.VehicleStopStatus.STOPPED_AT ? STOP_LOCATION_PROVIDER.getStopLocation(stopStatus.stopId) : Optional.empty();
        } catch (Exception e) {
            maybeStopLocation = Optional.empty();
        }

        //Ignore messages where the vehicle has no location or location cannot be determined from stop status
        if ((!hfpData.getPayload().hasLat() || !hfpData.getPayload().hasLong()) && !maybeStopLocation.isPresent()) {
            return Optional.empty();
        }

        GtfsRealtime.VehiclePosition.Builder vp = GtfsRealtime.VehiclePosition.newBuilder();

        vp.setTimestamp(hfpData.getPayload().getTsi());
        //Do not set stop status if the final stop has been reached or the next stop is outside HSL area
        if (stopStatus != null) {
            vp.setCurrentStatus(stopStatus.stopStatus);
            vp.setStopId(stopStatus.stopId);
        }

        Optional<Double> stopLatitude = maybeStopLocation.map(location -> location[0]);
        Optional<Double> stopLongitude = maybeStopLocation.map(location -> location[1]);

        vp.setPosition(GtfsRealtime.Position.newBuilder()
                .setLatitude((float)(hfpData.getPayload().hasLat() ? hfpData.getPayload().getLat() : stopLatitude.get()))
                .setLongitude((float)(hfpData.getPayload().hasLong() ? hfpData.getPayload().getLong() : stopLongitude.get()))
                .setSpeed((float) hfpData.getPayload().getSpd())
                .setBearing(hfpData.getPayload().getHdg())
                .setOdometer(hfpData.getPayload().getOdo()));

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
