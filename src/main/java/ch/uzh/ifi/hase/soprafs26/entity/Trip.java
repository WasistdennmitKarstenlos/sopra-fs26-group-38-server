package ch.uzh.ifi.hase.soprafs26.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Date;

/**
 * Trip represents a group trip planning room.
 * Users can create trips, invite others, propose destinations and activities.
 */
@Entity
@Table(name = "TRIP")
public class Trip implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String roomCode;

    @Column(nullable = false)
    private Long hostId; // Reference to User who created this trip

    @Column(nullable = false, updatable = false)
    private Date creationDate;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private TripStatus status; // ACTIVE, EVALUATION, FINALIZED

    @Column
    private Long finalDestinationId; // ID of the selected destination (after final evaluation)

    // Constructors
    public Trip() {
        this.status = TripStatus.ACTIVE;
        this.creationDate = new Date();
    }

    public Trip(String name, String roomCode, Long hostId) {
        this.name = name;
        this.roomCode = roomCode;
        this.hostId = hostId;
        this.status = TripStatus.ACTIVE;
        this.creationDate = new Date();
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

    public TripStatus getStatus() {
        return status;
    }

    public void setStatus(TripStatus status) {
        this.status = status;
    }

    public Long getFinalDestinationId() {
        return finalDestinationId;
    }

    public void setFinalDestinationId(Long finalDestinationId) {
        this.finalDestinationId = finalDestinationId;
    }

    // Nested enum for Trip status
    public enum TripStatus {
        ACTIVE,           // Accepting destinations and activities
        EVALUATION,       // In evaluation mode, read-only
        FINALIZED         // Final destination selected, fully read-only
    }
}
