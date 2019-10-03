package fi.hsl.transitdata.vehicleposition.application.utils;

import fi.hsl.common.hfp.proto.Hfp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class VehicleTimestampValidator {
    private static final Logger log = LoggerFactory.getLogger(VehicleTimestampValidator.class);

    //Cache for storing latest timestamp received from the vehicle
    //This is used to only publish the vehicle position with latest timestamp
    private Map<String, Long> vehicleTimestamps = new HashMap<>(1000);

    private final long maxTimeDifferenceSeconds;

    public VehicleTimestampValidator(long maxTimeDifference) {
        this.maxTimeDifferenceSeconds = maxTimeDifference;
    }

    public boolean validateTimestamp(Hfp.Data hfpData, long pulsarEventTimeMs) {
        long timeDifferenceSeconds = hfpData.getPayload().getTsi() - pulsarEventTimeMs / 1000;

        if (timeDifferenceSeconds <= maxTimeDifferenceSeconds) {
            //Only publish vehicle position with latest timestamp
            return vehicleTimestamps.compute(hfpData.getTopic().getUniqueVehicleId(), (key, value) -> {
                if (value == null || value < hfpData.getPayload().getTsi()) {
                    return hfpData.getPayload().getTsi();
                } else {
                    return value;
                }
            }) == hfpData.getPayload().getTsi();
        } else {
            //Discard vehicle positions if timestamp is too much in the future
            log.warn("Vehicle {} had timestamp {} seconds in future (vehicle: {}, current time: {})", hfpData.getTopic().getUniqueVehicleId(), timeDifferenceSeconds, hfpData.getPayload().getTsi(), pulsarEventTimeMs / 1000);
            return false;
        }
    }
}
