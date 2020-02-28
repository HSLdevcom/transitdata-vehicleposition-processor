package fi.hsl.transitdata.vehicleposition.application;

import com.google.transit.realtime.GtfsRealtime;
import fi.hsl.common.hfp.proto.Hfp;

import java.util.HashMap;
import java.util.Map;

public class StopStatusProcessor {
    private final Map<String, StopStatus> vehicleStopStatus = new HashMap<>(1000);

    public StopStatus getStopStatus(Hfp.Data hfpData) {
        return vehicleStopStatus.compute(hfpData.getTopic().getUniqueVehicleId(), (uniqueVehicleId, previousStopStatus) -> processStopStatus(previousStopStatus, hfpData));
    }

    private StopStatus processStopStatus(StopStatus previousStopStatus, Hfp.Data hfpData) {
        //If vehicle has reached its final stop or left HSL area (next_stop being empty), remove it from the list
        if ("EOL".equals(hfpData.getTopic().getNextStop()) || hfpData.getTopic().getNextStop().isEmpty()) {
            return null;
        }

        if (hfpData.getTopic().getTransportMode() == Hfp.Topic.TransportMode.metro || hfpData.getTopic().getTransportMode() == Hfp.Topic.TransportMode.ferry) {
            return processStopStatusWithoutEvents(hfpData);
        }

        if (previousStopStatus == null || hfpData.getTopic().getEventType() == Hfp.Topic.EventType.PDE || hfpData.getTopic().getEventType() == Hfp.Topic.EventType.PAS) {
            //Set StopStatus to IN_TRANSIT_TO when a vehicle departs from a stop or passes through a stop
            return new StopStatus(hfpData.getTopic().getNextStop(), GtfsRealtime.VehiclePosition.VehicleStopStatus.IN_TRANSIT_TO);
        }

        if (hfpData.getTopic().getEventType() == Hfp.Topic.EventType.DUE) {
            //Set StopStatus to INCOMING_AT when a vehicle is just about to arrive to a stop
            return new StopStatus(hfpData.getTopic().getNextStop(), GtfsRealtime.VehiclePosition.VehicleStopStatus.INCOMING_AT);
        }

        if (hfpData.getTopic().getEventType() == Hfp.Topic.EventType.ARS) {
            //Set StopStatus to STOPPED_AT when a vehicle has arrived to a stop
            return new StopStatus(hfpData.getTopic().getNextStop(), GtfsRealtime.VehiclePosition.VehicleStopStatus.STOPPED_AT);
        }

        //If a vehicle was arriving to a stop and the stop id in the topic has not changed, return previous status
        if (previousStopStatus.stopStatus == GtfsRealtime.VehiclePosition.VehicleStopStatus.INCOMING_AT &&
                hfpData.getTopic().getNextStop().equals(previousStopStatus.stopId)) {
            return previousStopStatus;
        }

        if (previousStopStatus.stopStatus == GtfsRealtime.VehiclePosition.VehicleStopStatus.STOPPED_AT) {
            if (hfpData.getPayload().hasStop() && String.valueOf(hfpData.getPayload().getStop()).equals(hfpData.getTopic().getNextStop())) {
                //If next_stop has not changed in the topic, the vehicle is still at a stop
                return previousStopStatus;
            } else {
                //If next_stop has changed, the vehicle is in transit to the next stop
                return new StopStatus(hfpData.getTopic().getNextStop(), GtfsRealtime.VehiclePosition.VehicleStopStatus.IN_TRANSIT_TO);
            }
        }

        return new StopStatus(hfpData.getTopic().getNextStop(), GtfsRealtime.VehiclePosition.VehicleStopStatus.IN_TRANSIT_TO);
    }

    private StopStatus processStopStatusWithoutEvents(Hfp.Data data) {
        if (data.getPayload().hasStop() && String.valueOf(data.getPayload().getStop()).equals(data.getTopic().getNextStop())) {
            return new StopStatus(String.valueOf(data.getPayload().getStop()), GtfsRealtime.VehiclePosition.VehicleStopStatus.STOPPED_AT);
        } else {
            return new StopStatus(data.getTopic().getNextStop(), GtfsRealtime.VehiclePosition.VehicleStopStatus.IN_TRANSIT_TO);
        }
    }

    public static class StopStatus {
        public final String stopId;
        public final GtfsRealtime.VehiclePosition.VehicleStopStatus stopStatus;

        public StopStatus(String stopId, GtfsRealtime.VehiclePosition.VehicleStopStatus stopStatus) {
            this.stopId = stopId;
            this.stopStatus = stopStatus;
        }
    }
}
