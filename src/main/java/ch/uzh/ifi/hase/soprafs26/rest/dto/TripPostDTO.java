package ch.uzh.ifi.hase.soprafs26.rest.dto;

import java.io.Serializable;

/**
 * TripPostDTO for creating a new trip
 * Client sends only the trip name
 */
public class TripPostDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String name;
    private String imageBase64;

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

    public String getImageBase64() {
        return imageBase64;
    }

    public void setImageBase64(String imageBase64) {
        this.imageBase64 = imageBase64;
    }
}
