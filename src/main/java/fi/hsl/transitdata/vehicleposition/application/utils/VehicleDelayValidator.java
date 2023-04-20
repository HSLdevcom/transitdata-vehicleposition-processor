package fi.hsl.transitdata.vehicleposition.application.utils;

import fi.hsl.common.hfp.proto.Hfp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VehicleDelayValidator {

    private static final Logger log = LoggerFactory.getLogger(VehicleDelayValidator.class);

    private final long maximumDelayInSeconds;

    public VehicleDelayValidator(long maximumDelayInSeconds) {
        this.maximumDelayInSeconds = maximumDelayInSeconds;
    }

    public boolean validateDelay(Hfp.Data hfpData) {
        if (maximumDelayInSeconds == 0) {
            return true;
        }

        if (hfpData.getPayload().getDl() < maximumDelayInSeconds) {
            return true;
        }

        log.debug("Vehicle {} had delay (dl) too big (vehicle: {}, Maximum delay allowed: {})", hfpData.getTopic().getUniqueVehicleId(), hfpData.getPayload().getDl(), maximumDelayInSeconds);
        return false;
    }
}
