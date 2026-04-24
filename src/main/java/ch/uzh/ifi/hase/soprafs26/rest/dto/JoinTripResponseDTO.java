package ch.uzh.ifi.hase.soprafs26.rest.dto;

import java.io.Serializable;

public class JoinTripResponseDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String roomCode;
    private Long tripId;
    private Long userId;

    public String getRoomCode() {
        return roomCode;
    }

    public void setRoomCode(String roomCode) {
        this.roomCode = roomCode;
    }

    public Long getTripId() {
        return tripId;
    }

    public void setTripId(Long tripId) {
        this.tripId = tripId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}
