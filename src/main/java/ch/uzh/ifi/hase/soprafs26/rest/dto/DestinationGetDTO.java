package ch.uzh.ifi.hase.soprafs26.rest.dto;

import java.io.Serializable;
import java.util.List;

public class DestinationGetDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long tripId;
    private String destinationName;
    private Long proposedByUserId;
    private List<ActivitySearchResultDTO> activities;

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

    public String getDestinationName() {
        return destinationName;
    }

    public void setDestinationName(String destinationName) {
        this.destinationName = destinationName;
    }

    public Long getProposedByUserId() {
        return proposedByUserId;
    }

    public void setProposedByUserId(Long proposedByUserId) {
        this.proposedByUserId = proposedByUserId;
    }

    public List<ActivitySearchResultDTO> getActivities() {
        return activities;
    }

    public void setActivities(List<ActivitySearchResultDTO> activities) {
        this.activities = activities;
    }
}
