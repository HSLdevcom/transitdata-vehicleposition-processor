package fi.hsl.transitdata.vehicleposition.application;

import fi.hsl.common.hfp.proto.Hfp;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.zone.ZoneRules;
import java.util.Date;

public class TimeUtils {
    private static final ZoneId TZ = ZoneId.of("Europe/Helsinki");

    public static String getStartTime(Hfp.Data data) {
        //Implementation based on https://digitransit.fi/en/developers/apis/1-routing-api/routes/#a-namefuzzytripaquery-a-trip-without-its-id
        final String timeZonedTst = ZonedDateTime.ofInstant(Instant.ofEpochSecond(data.getPayload().getTsi()), ZoneOffset.UTC).withZoneSameInstant(TZ).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);

        final String oday = data.getPayload().getOday();
        final String tstDay = timeZonedTst.substring(0, 10);

        if (oday.equals(tstDay)) {
            return data.getPayload().getStart()+":00";
        } else {
            int tstTime = hhMmToSeconds(timeZonedTst.substring(11, 16));
            int startTime = hhMmToSeconds(data.getPayload().getStart());

            if (startTime < tstTime) {
                startTime += 86400;
            }

            return (startTime / 3600) + ":" + ((startTime % 3600) / 60) + ":00";
        }
    }

    public static int hhMmToSeconds(String hhMm) {
        return Integer.parseInt(hhMm.substring(0, 2)) * 60 * 60 + Integer.parseInt(hhMm.substring(3, 5)) * 60;
    }
}
