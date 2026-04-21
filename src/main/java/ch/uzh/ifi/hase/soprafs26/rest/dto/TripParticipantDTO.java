package ch.uzh.ifi.hase.soprafs26.rest.dto;

import java.io.Serializable;

public class TripParticipantDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long userId;
    private String roomUsername;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getRoomUsername() {
        return roomUsername;
    }

    public void setRoomUsername(String roomUsername) {
        this.roomUsername = roomUsername;
    }
}