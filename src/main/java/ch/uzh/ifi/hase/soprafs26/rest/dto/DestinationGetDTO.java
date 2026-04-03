package ch.uzh.ifi.hase.soprafs26.rest.dto;

import java.io.Serializable;

public class DestinationGetDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long tripId;
    private String name;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTripId() {
        return tripId;
    }

    public void setTripId(Long tripId) {
        this.tripId = tripId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}