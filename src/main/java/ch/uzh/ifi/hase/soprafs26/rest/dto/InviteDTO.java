package ch.uzh.ifi.hase.soprafs26.rest.dto;

import java.io.Serializable;

/**
 * InviteDTO for returning invite information to the client
 * Contains the room code that can be shared with other users to join the trip
 */
public class InviteDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String roomCode;
    private String inviteLink; // Optional: full invite link

    // Constructors
    public InviteDTO() {
    }

    public InviteDTO(String roomCode) {
        this.roomCode = roomCode;
    }

    public InviteDTO(String roomCode, String inviteLink) {
        this.roomCode = roomCode;
        this.inviteLink = inviteLink;
    }

    // Getters and Setters
    public String getRoomCode() {
        return roomCode;
    }

    public void setRoomCode(String roomCode) {
        this.roomCode = roomCode;
    }

    public String getInviteLink() {
        return inviteLink;
    }

    public void setInviteLink(String inviteLink) {
        this.inviteLink = inviteLink;
    }
}
