package ch.uzh.ifi.hase.soprafs26.rest.dto;

import ch.uzh.ifi.hase.soprafs26.entity.Trip;
import java.io.Serializable;
import java.util.Date;

/**
 * TripGetDTO for returning trip data to the client
 * Contains all trip information
 */
public class TripGetDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private String roomCode;
    private Long hostId;
    private Date creationDate;
    private String status; // String representation of Trip.TripStatus
    private Long finalDestinationId;

    // Constructors
    public TripGetDTO() {
    }

    public TripGetDTO(Long id, String name, String roomCode, Long hostId, Date creationDate, 
                      String status, Long finalDestinationId) {
        this.id = id;
        this.name = name;
        this.roomCode = roomCode;
        this.hostId = hostId;
        this.creationDate = creationDate;
        this.status = status;
        this.finalDestinationId = finalDestinationId;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRoomCode() {
        return roomCode;
    }

    public void setRoomCode(String roomCode) {
        this.roomCode = roomCode;
    }

    public Long getHostId() {
        return hostId;
    }

    public void setHostId(Long hostId) {
        this.hostId = hostId;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getFinalDestinationId() {
        return finalDestinationId;
    }

    public void setFinalDestinationId(Long finalDestinationId) {
        this.finalDestinationId = finalDestinationId;
    }
}
