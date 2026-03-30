package ch.uzh.ifi.hase.soprafs26.rest.dto;

import java.io.Serializable;

/**
 * TripPostDTO for creating a new trip
 * Client sends only the trip name
 */
public class TripPostDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String name;

    // Constructors
    public TripPostDTO() {
    }

    public TripPostDTO(String name) {
        this.name = name;
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
