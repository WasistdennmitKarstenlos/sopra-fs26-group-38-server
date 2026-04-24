package ch.uzh.ifi.hase.soprafs26.event;

import org.springframework.context.ApplicationEvent;

public class TripStatusUpdatedEvent extends ApplicationEvent {
    private final Long tripId;
    private final String status;

    public TripStatusUpdatedEvent(Object source, Long tripId, String status) {
        super(source);
        this.tripId = tripId;
        this.status = status;
    }

    public Long getTripId() {
        return tripId;
    }

    public String getStatus() {
        return status;
    }
}
