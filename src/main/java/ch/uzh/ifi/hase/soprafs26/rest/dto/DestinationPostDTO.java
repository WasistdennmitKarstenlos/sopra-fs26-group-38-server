package ch.uzh.ifi.hase.soprafs26.rest.dto;

import java.io.Serializable;

public class DestinationPostDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String locationName;

    public String getLocationName() {
        return locationName;
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }
}
