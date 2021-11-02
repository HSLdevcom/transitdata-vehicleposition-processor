package fi.hsl.transitdata.vehicleposition.application.gtfsrt;

import com.google.transit.realtime.GtfsRealtime;
import fi.hsl.common.hfp.proto.Hfp;
import fi.hsl.common.passengercount.proto.PassengerCount;

import java.util.NavigableMap;
import java.util.Optional;

public class GtfsRtOccupancyStatusHelper {
    private final NavigableMap<Integer, GtfsRealtime.VehiclePosition.OccupancyStatus> occuToOccupancyStatus;
    private final NavigableMap<Double, GtfsRealtime.VehiclePosition.OccupancyStatus> loadRatioToOccupancyStatus;

    public GtfsRtOccupancyStatusHelper(NavigableMap<Integer, GtfsRealtime.VehiclePosition.OccupancyStatus> occuToOccupancyStatus, NavigableMap<Double, GtfsRealtime.VehiclePosition.OccupancyStatus> loadRatioToOccupancyStatus) {
        this.occuToOccupancyStatus = occuToOccupancyStatus;
        this.loadRatioToOccupancyStatus = loadRatioToOccupancyStatus;
    }

    public Optional<GtfsRealtime.VehiclePosition.OccupancyStatus> getOccupancyStatus(Hfp.Payload hfpPayload, PassengerCount.Payload passengerCountPayload) {
        //If occu field is 100, the driver has marked the vehicle as full
        if (hfpPayload.getOccu() == 100) {
            return Optional.of(GtfsRealtime.VehiclePosition.OccupancyStatus.FULL);
        }

        if (passengerCountPayload != null) {
            return Optional.of(loadRatioToOccupancyStatus.lowerEntry(passengerCountPayload.getVehicleCounts().getVehicleLoadRatio()).getValue());
        }

        //If passenger count from APC message is not available, but occu contains value other than 0, use that
        //Currently occu is only available for Suomenlinna ferries
        if (hfpPayload.getOccu() != 0) {
            return Optional.of(occuToOccupancyStatus.lowerEntry(hfpPayload.getOccu()).getValue());
        }

        return Optional.empty();
    }
}
