package fi.hsl.transitdata.vehicleposition.application.gtfsrt;

import com.dslplatform.json.runtime.MapAnalyzer;
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
        
        boolean containsId = false;
        
        if (passengerCountEnabledVehicles != null) {
            if (hfpPayload == null) {
                throw new RuntimeException("hfpPayload is null");
            }
    
            int oper = hfpPayload.getOper();
            int veh = hfpPayload.getVeh();
            
            try {
                containsId = passengerCountEnabledVehicles.contains(oper + "/" + veh);
            } catch (Exception x) {
                log.error("Contains failed (oper=" + oper + ", veh=" + veh + ")");
                throw new RuntimeException("Contains failed (oper=" + oper + ", veh=" + veh + ")", x);
            }
        }
        
        if (passengerCountEnabledVehicles == null || containsId) {
            //If occu field is 100, the driver has marked the vehicle as full
            try {
                if (hfpPayload.getOccu() == 100) {
                    return Optional.of(GtfsRealtime.VehiclePosition.OccupancyStatus.FULL);
                }
            } catch (Exception x) {
                log.error("GetOccu 1 failed");
                throw new RuntimeException("GetOccu 1 failed", x);
            }

            if (passengerCountPayload != null) {
                try {
                    if (passengerCountPayload.getVehicleCounts().hasVehicleLoad() && passengerCountPayload.getVehicleCounts().getVehicleLoad() == 0) {
                        //If vehicle load is zero, vehicle load ratio is unavailable and the vehicle is empty
                        return Optional.of(GtfsRealtime.VehiclePosition.OccupancyStatus.EMPTY);
                    }
                } catch (Exception x) {
                    log.error("passengerCountPayload 1 failed");
                    throw new RuntimeException("passengerCountPayload 1 failed", x);
                }
    
                double vehicleLoadRatio;
                
                try {
                    vehicleLoadRatio = passengerCountPayload.getVehicleCounts().getVehicleLoadRatio();
                } catch (Exception x) {
                    log.error("getVehicleLoadRatio() failed");
                    throw new RuntimeException("getVehicleLoadRatio() failed", x);
                }
    
                Map.Entry<Double, GtfsRealtime.VehiclePosition.OccupancyStatus> doubleOccupancyStatusEntry = null;
                
                try {
                    doubleOccupancyStatusEntry = loadRatioToOccupancyStatus.lowerEntry(vehicleLoadRatio);
                } catch (Exception x) {
                    log.error("lowerEntry(vehicleLoadRatio) failed (vehicleLoadRatio=" + vehicleLoadRatio + ")");
                    throw new RuntimeException("lowerEntry(vehicleLoadRatio) failed (vehicleLoadRatio=" + vehicleLoadRatio + ")", x);
                }
                
                if (doubleOccupancyStatusEntry == null) {
                    log.error("doubleOccupancyStatusEntry is null. vehicleLoadRatio=" + vehicleLoadRatio);
                    throw new RuntimeException("doubleOccupancyStatusEntry is null. vehicleLoadRatio=" + vehicleLoadRatio);
                }
    
                GtfsRealtime.VehiclePosition.OccupancyStatus occupancyStatus = null;
                
                try {
                    occupancyStatus = doubleOccupancyStatusEntry.getValue();
                } catch (Exception x) {
                    log.error("getValue() failed");
                    throw new RuntimeException("getValue() failed (Key=" + doubleOccupancyStatusEntry.getKey() + ", Value=" + doubleOccupancyStatusEntry.getValue() + ")", x);
                }
    
                if (occupancyStatus == null) {
                    log.error("occupancyStatus is null");
                    throw new RuntimeException("occupancyStatus is null");
                }
                
                try {
                    return Optional.of(occupancyStatus);
                } catch (Exception x) {
                    log.error("passengerCountPayload 2 failed (occupancyStatus=" + occupancyStatus + ")");
                    throw new RuntimeException("passengerCountPayload 2 failed (occupancyStatus=" + occupancyStatus + ")", x);
                }
            }

            //If passenger count from APC message is not available, but occu contains value other than 0, use that
            //Currently occu is only available for Suomenlinna ferries
            try {
                if (hfpPayload.getOccu() > 0) {
                    return Optional.of(occuToOccupancyStatus.lowerEntry(hfpPayload.getOccu()).getValue());
                } else {
                    return Optional.of(GtfsRealtime.VehiclePosition.OccupancyStatus.EMPTY);
                }
            } catch (Exception x) {
                throw new RuntimeException("GetOccu 2 failed", x);
            }
        }

        return Optional.empty();
    }
}
