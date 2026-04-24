package ch.uzh.ifi.hase.soprafs26.rest.dto;

import java.io.Serializable;

public class TripParticipantDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long userId;
    private String username;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}