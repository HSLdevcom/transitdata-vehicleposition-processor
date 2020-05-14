package fi.hsl.transitdata.vehicleposition.application.utils;

import fi.hsl.common.hfp.proto.Hfp;
import fi.hsl.transitdata.vehicleposition.application.utils.VehicleTimestampValidator;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class VehicleTimestampValidatorTest {
    private VehicleTimestampValidator validator;

    @Before
    public void setup() {
        validator = new VehicleTimestampValidator(5);
    }

    @Test
    public void testVehiclePositionWithTimestampInFutureIsIgnored() {
        Hfp.Data data = Hfp.Data.newBuilder()
                .setSchemaVersion(1)
                .setTopic(Hfp.Topic.newBuilder()
                    .setSchemaVersion(1)
                    .setUniqueVehicleId("1/1")
                    .setReceivedAt(0)
                    .setTopicPrefix("hfp")
                    .setTopicVersion("v2")
                    .setJourneyType(Hfp.Topic.JourneyType.journey)
                    .setTemporalType(Hfp.Topic.TemporalType.ongoing)
                    .setOperatorId(1)
                    .setVehicleNumber(1))
                .setPayload(Hfp.Payload.newBuilder()
                    .setSchemaVersion(1)
                    .setTst("")
                    .setTsi(10))
                .build();

        assertFalse(validator.validateTimestamp(data, 3000));
    }

    @Test
    public void testVehiclePositionWithOlderTimestampThanPreviousIsIgnored() {
        Hfp.Data data1 = Hfp.Data.newBuilder()
                .setSchemaVersion(1)
                .setTopic(Hfp.Topic.newBuilder()
                        .setSchemaVersion(1)
                        .setUniqueVehicleId("1/1")
                        .setReceivedAt(0)
                        .setTopicPrefix("hfp")
                        .setTopicVersion("v2")
                        .setJourneyType(Hfp.Topic.JourneyType.journey)
                        .setTemporalType(Hfp.Topic.TemporalType.ongoing)
                        .setOperatorId(1)
                        .setVehicleNumber(1))
                .setPayload(Hfp.Payload.newBuilder()
                        .setSchemaVersion(1)
                        .setTst("")
                        .setTsi(10))
                .build();

        assertTrue(validator.validateTimestamp(data1, 10500));

        Hfp.Data data2 = Hfp.Data.newBuilder()
                .setSchemaVersion(1)
                .setTopic(Hfp.Topic.newBuilder()
                        .setSchemaVersion(1)
                        .setUniqueVehicleId("1/1")
                        .setReceivedAt(0)
                        .setTopicPrefix("hfp")
                        .setTopicVersion("v2")
                        .setJourneyType(Hfp.Topic.JourneyType.journey)
                        .setTemporalType(Hfp.Topic.TemporalType.ongoing)
                        .setOperatorId(1)
                        .setVehicleNumber(1))
                .setPayload(Hfp.Payload.newBuilder()
                        .setSchemaVersion(1)
                        .setTst("")
                        .setTsi(9))
                .build();

        //Vehicle position is ignored
        assertFalse(validator.validateTimestamp(data2, 11500));

        Hfp.Data data3 = Hfp.Data.newBuilder()
                .setSchemaVersion(1)
                .setTopic(Hfp.Topic.newBuilder()
                        .setSchemaVersion(1)
                        .setUniqueVehicleId("1/1")
                        .setReceivedAt(0)
                        .setTopicPrefix("hfp")
                        .setTopicVersion("v2")
                        .setJourneyType(Hfp.Topic.JourneyType.journey)
                        .setTemporalType(Hfp.Topic.TemporalType.ongoing)
                        .setOperatorId(1)
                        .setVehicleNumber(1))
                .setPayload(Hfp.Payload.newBuilder()
                        .setSchemaVersion(1)
                        .setTst("")
                        .setTsi(12))
                .build();

        assertTrue(validator.validateTimestamp(data3, 12500));
    }
}
