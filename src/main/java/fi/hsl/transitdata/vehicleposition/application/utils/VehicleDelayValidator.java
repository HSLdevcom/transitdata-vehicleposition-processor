package fi.hsl.transitdata.vehicleposition.application.utils;

import fi.hsl.common.hfp.proto.Hfp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VehicleDelayValidator {

    private static final Logger log = LoggerFactory.getLogger(VehicleDelayValidator.class);

    private final long maximumDelay;

    public VehicleDelayValidator(long maximumDelay) {
        this.maximumDelay = maximumDelay;
    }

    public boolean validateDelay(Hfp.Data hfpData) {

        return true;
    }
}
