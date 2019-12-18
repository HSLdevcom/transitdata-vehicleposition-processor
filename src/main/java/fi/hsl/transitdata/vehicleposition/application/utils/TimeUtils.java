package fi.hsl.transitdata.vehicleposition.application.utils;

import fi.hsl.common.hfp.proto.Hfp;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class TimeUtils {
    private static final ZoneId TZ = ZoneId.of("Europe/Helsinki");

    public static String getStartTime(Hfp.Data data) {
        //Implementation based on https://digitransit.fi/en/developers/apis/1-routing-api/routes/#a-namefuzzytripaquery-a-trip-without-its-id
        final ZonedDateTime timeZonedTst = ZonedDateTime.ofInstant(Instant.ofEpochSecond(data.getPayload().getTsi()), ZoneOffset.UTC).withZoneSameInstant(TZ);
        final String formattedTimeZonedTst = timeZonedTst.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);

        final String oday = data.getPayload().getOday();
        final String tstDay = formattedTimeZonedTst.substring(0, 10);

        final ZonedDateTime startTimeOday = LocalDate.parse(oday).atStartOfDay(TZ).plus(hhMmToSeconds(data.getPayload().getStart()), ChronoUnit.SECONDS);

        if (oday.equals(tstDay) &&
                //If start time would be more than 12 hours in the past, assume that the trip begins on the following day
                startTimeOday.until(timeZonedTst, ChronoUnit.HOURS) <= -12) {
            return data.getPayload().getStart()+":00";
        } else {
            int tstTime = hhMmToSeconds(formattedTimeZonedTst.substring(11, 16));
            int startTime = hhMmToSeconds(data.getPayload().getStart());

            if (startTime <= tstTime) {
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
