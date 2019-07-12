package fi.transitdata.vehicleposition.application.utils;

import fi.hsl.common.hfp.proto.Hfp;
import fi.hsl.transitdata.vehicleposition.application.utils.TimeUtils;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TimeUtilsTest {
    @Test
    public void testParseTime1() {
        assertEquals(56100, TimeUtils.hhMmToSeconds("15:35"));
    }

    @Test
    public void testParseTime2() {
        assertEquals(32400, TimeUtils.hhMmToSeconds("09:00"));
    }

    @Test
    public void testGetStartTimeSameDay() {
        Hfp.Data data = Hfp.Data.newBuilder()
            .setSchemaVersion(1)
            .setPayload(Hfp.Payload.newBuilder()
                .setSchemaVersion(1)
                .setOday("2019-06-28")
                .setTst("2019-06-28T09:49:01.457Z")
                .setTsi(0)
                .setStart("11:57"))
            .build();

        assertEquals("11:57:00", TimeUtils.getStartTime(data));
    }

    @Test
    public void testGetStartTimeDifferentDayEarlierStart() {
        Hfp.Data data = Hfp.Data.newBuilder()
                .setSchemaVersion(1)
                .setPayload(Hfp.Payload.newBuilder()
                        .setSchemaVersion(1)
                        .setOday("2019-06-27")
                        .setTst("2019-06-28T00:50:25.000Z")
                        .setTsi(1561683025)
                        .setStart("03:45"))
                .build();

        assertEquals("27:45:00", TimeUtils.getStartTime(data));
    }

    @Test
    public void testGetStartTimeDifferentDayLaterStart() {
        Hfp.Data data = Hfp.Data.newBuilder()
                .setSchemaVersion(1)
                .setPayload(Hfp.Payload.newBuilder()
                        .setSchemaVersion(1)
                        .setOday("2019-06-27")
                        .setTst("2019-06-27T22:50:25.000Z")
                        .setTsi(1561683025)
                        .setStart("23:55"))
                .build();

        assertEquals("23:55:00", TimeUtils.getStartTime(data));
    }
}
