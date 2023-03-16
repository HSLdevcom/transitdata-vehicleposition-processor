package fi.hsl.transitdata.vehicleposition.application.utils;

import fi.hsl.common.hfp.proto.Hfp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VehicleDelayValidator {

    private static final Logger log = LoggerFactory.getLogger(VehicleDelayValidator.class);

    private final long maximumDelayInSeconds;
    private final boolean isEnabled;

    public VehicleDelayValidator(long maximumDelayInSeconds, boolean isEnabled) {
        this.maximumDelayInSeconds = maximumDelayInSeconds;
        this.isEnabled = isEnabled;
    }

    public boolean validateDelay(Hfp.Data hfpData) {
        if (!isEnabled) {
            return true;
        }

        if (hfpData.getPayload().getDl() < maximumDelayInSeconds) {
            return true;
        }

        log.warn("Vehicle {} had delay (dl) too big (vehicle: {}, Maximum delay allowed: {})", hfpData.getTopic().getUniqueVehicleId(), hfpData.getPayload().getTsi(), maximumDelayInSeconds);
        return false;
    }
}
