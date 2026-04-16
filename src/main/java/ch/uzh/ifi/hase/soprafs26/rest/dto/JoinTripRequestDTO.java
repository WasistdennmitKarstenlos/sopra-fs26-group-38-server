package ch.uzh.ifi.hase.soprafs26.rest.dto;

import java.io.Serializable;

public class JoinTripRequestDTO implements Serializable{
    private static final long serialVersionUID = 1L;
    
    private String roomCode;
    private String roomUsername;

    public String getRoomCode() {
        return roomCode;
    }

    public void setRoomCode(String roomCode) {
        this.roomCode = roomCode;
    }

    public String getRoomUsername() {
        return roomUsername;
    }

    public void setRoomUsername(String roomUsername) {
        this.roomUsername = roomUsername;
    }
}
