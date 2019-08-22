package fi.hsl.transitdata.vehicleposition.application.utils;

import fi.hsl.common.hfp.proto.Hfp;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

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

            return formatTime(startTime);
        }
    }

    public static int hhMmToSeconds(String hhMm) {
        return Integer.parseInt(hhMm.substring(0, 2)) * 60 * 60 + Integer.parseInt(hhMm.substring(3, 5)) * 60;
    }

    public static String formatTime(int time) {
        String hours = String.valueOf(time / 3600);
        if (hours.length() < 2) {
            hours = "0"+hours;
        }

        String minutes = String.valueOf((time % 3600) / 60);
        if (minutes.length() < 2) {
            minutes = "0"+minutes;
        }

        return String.join(":", hours, minutes, "00");
    }
}
