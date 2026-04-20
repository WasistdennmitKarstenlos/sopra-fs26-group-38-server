package ch.uzh.ifi.hase.soprafs26.event;

import org.springframework.context.ApplicationEvent;

public class DestinationVotesUpdatedEvent extends ApplicationEvent {
    private final Long tripId;
    private final Long userId;

    public DestinationVotesUpdatedEvent(Object source, Long tripId, Long userId) {
        super(source);
        this.tripId = tripId;
        this.userId = userId;
    }

    public Long getTripId() {
        return tripId;
    }

    public Long getUserId() {
        return userId;
    }
}
