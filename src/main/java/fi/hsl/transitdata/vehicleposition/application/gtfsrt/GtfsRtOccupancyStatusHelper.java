package fi.hsl.transitdata.vehicleposition.application.gtfsrt;

import com.google.transit.realtime.GtfsRealtime;
import fi.hsl.common.hfp.proto.Hfp;
import fi.hsl.common.passengercount.proto.PassengerCount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class GtfsRtOccupancyStatusHelper {
    private static final Logger log = LoggerFactory.getLogger(GtfsRtOccupancyStatusHelper.class);

    private final NavigableMap<Integer, GtfsRealtime.VehiclePosition.OccupancyStatus> occuToOccupancyStatus;
    private final NavigableMap<Double, GtfsRealtime.VehiclePosition.OccupancyStatus> loadRatioToOccupancyStatus;
    private final Set<String> passengerCountEnabledVehicles; //null if no filtering is used (i.e. publish passenger count for all vehicles)

    /**
     *
     * @param occuToOccupancyStatus
     * @param loadRatioToOccupancyStatus
     * @param passengerCountEnabledVehicles Comma-separated list of vehicles for which passenger count is enabled, empty or null list -> no filtering
     */
    public GtfsRtOccupancyStatusHelper(NavigableMap<Integer, GtfsRealtime.VehiclePosition.OccupancyStatus> occuToOccupancyStatus,
                                       NavigableMap<Double, GtfsRealtime.VehiclePosition.OccupancyStatus> loadRatioToOccupancyStatus,
                                       Collection<String> passengerCountEnabledVehicles) {
        this.occuToOccupancyStatus = occuToOccupancyStatus;
        this.loadRatioToOccupancyStatus = loadRatioToOccupancyStatus;
        this.passengerCountEnabledVehicles = (passengerCountEnabledVehicles == null || passengerCountEnabledVehicles.isEmpty()) ? null : new HashSet<>(passengerCountEnabledVehicles);

        if (this.passengerCountEnabledVehicles == null) {
            log.info("Occupancy status enabled for all vehicles");
        } else {
            log.info("Occupancy status enabled for vehicles: {}", String.join(", ", passengerCountEnabledVehicles));
        }
    }

    public GtfsRtOccupancyStatusHelper(NavigableMap<Integer, GtfsRealtime.VehiclePosition.OccupancyStatus> occuToOccupancyStatus,
                                       NavigableMap<Double, GtfsRealtime.VehiclePosition.OccupancyStatus> loadRatioToOccupancyStatus) {
        this(occuToOccupancyStatus, loadRatioToOccupancyStatus, null);
    }

    public Optional<GtfsRealtime.VehiclePosition.OccupancyStatus> getOccupancyStatus(Hfp.Payload hfpPayload, PassengerCount.Payload passengerCountPayload) {
        if (passengerCountEnabledVehicles == null || passengerCountEnabledVehicles.contains(hfpPayload.getOper() + "/" + hfpPayload.getVeh())) {
            //If occu field is 100, the driver has marked the vehicle as full
            if (hfpPayload.getOccu() == 100) {
                return Optional.of(GtfsRealtime.VehiclePosition.OccupancyStatus.FULL);
            }

            if (passengerCountPayload != null) {
                if (passengerCountPayload.getVehicleCounts().hasVehicleLoad() && passengerCountPayload.getVehicleCounts().getVehicleLoad() == 0) {
                    //If vehicle load is zero, vehicle load ratio is unavailable and the vehicle is empty
                    return Optional.of(GtfsRealtime.VehiclePosition.OccupancyStatus.EMPTY);
                }

                return Optional.of(loadRatioToOccupancyStatus.lowerEntry(passengerCountPayload.getVehicleCounts().getVehicleLoadRatio()).getValue());
            }

            //If passenger count from APC message is not available, but occu contains value other than 0, use that
            //Currently occu is only available for Suomenlinna ferries
            if (hfpPayload.getOccu() > 0) {
                return Optional.of(occuToOccupancyStatus.lowerEntry(hfpPayload.getOccu()).getValue());
            } else {
                return Optional.of(GtfsRealtime.VehiclePosition.OccupancyStatus.EMPTY);
            }
        }

        return Optional.empty();
    }
}
