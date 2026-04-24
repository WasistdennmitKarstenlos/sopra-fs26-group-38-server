package ch.uzh.ifi.hase.soprafs26.rest.dto;

import java.io.Serializable;

public class JoinTripRequestDTO implements Serializable{
    private static final long serialVersionUID = 1L;
    
    private String roomCode;

    public String getRoomCode() {
        return roomCode;
    }

    public void setRoomCode(String roomCode) {
        this.roomCode = roomCode;
    }
}
