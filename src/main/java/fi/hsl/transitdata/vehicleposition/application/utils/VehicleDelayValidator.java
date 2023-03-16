package fi.hsl.transitdata.vehicleposition.application.utils;

import fi.hsl.common.hfp.proto.Hfp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VehicleDelayValidator {

    private static final Logger log = LoggerFactory.getLogger(VehicleDelayValidator.class);

    private final long maximumDelay;
    private final boolean isEnabled;

    public VehicleDelayValidator(long maximumDelay) {
        this.maximumDelay = maximumDelay;
        this.isEnabled = false;
    }

    public VehicleDelayValidator(long maximumDelay, boolean isEnabled) {
        this.maximumDelay = maximumDelay;
        this.isEnabled = isEnabled;
    }

    public boolean validateDelay(Hfp.Data hfpData) {
        if (!isEnabled) {
            return true;
        }

        if (hfpData.getPayload().getDl() < maximumDelay) {
            return true;
        }

        log.warn("Vehicle {} had delay (dl) too big (vehicle: {}, Maximum delay allowed: {})", hfpData.getTopic().getUniqueVehicleId(), hfpData.getPayload().getTsi(), maximumDelay);
        return false;
    }
}
