package ch.uzh.ifi.hase.soprafs26.entity;

import ch.uzh.ifi.hase.soprafs26.constant.TripStatus;
import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Internal Trip Representation.
 * This class models a trip and defines how it is persisted in the database.
 *
 * Field mapping notes:
 * - nullable = false -> value is required
 * - unique = true -> value must be unique across rows
 */
@Entity
@Table(name = "trips")
public class Trip implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue
	private Long tripId;

	@Column(nullable = false)
	private String tripName;

	@Column(nullable = false)
	private Long hostId;

	@Column(nullable = false, unique = true)
	private String roomCode;

	@Column(nullable = false)
	private LocalDateTime creationDate;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private TripStatus status;

	// @OneToMany(mappedBy = "trip", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
	// private List<TripMembership> memberships = new ArrayList<>();

	// @OneToMany(mappedBy = "trip", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
	// private List<Destination> destinations = new ArrayList<>();

	@PrePersist
	public void initializeDefaults() {
		if (creationDate == null) {
			creationDate = LocalDateTime.now();
		}
		if (status == null) {
			status = TripStatus.PLANNING;
		}
	}


    public Long getId() {
		return tripId;
	}

	public String getName() {
		return tripName;
	}

	public void setName(String name) {
		this.tripName = name;
	}

	public Long getHostId() {
		return hostId;
	}

	public String getRoomCode() {
		return roomCode;
	}

	public void setRoomCode(String roomCode) {
		this.roomCode = roomCode;
	}

	public LocalDateTime getCreationDate() {
		return creationDate;
	}

	public TripStatus getStatus() {
		return status;
	}

	public void setStatus(TripStatus status) {
		this.status = status;
	}

	public void joinTrip(long userId) {
	    // Placeholder for actual implementation
	}

    public List<Destination> getDestinations() {
		return null; // Placeholder for actual implementation
	}

	public void addDestination(Destination destination) {
	    // Placeholder for actual implementation
	}

	public void deleteDestination(long destinationId) {
	     // Placeholder for actual implementation
	}

	public void finalizeTrip() {
		status = TripStatus.FINALIZED;
	}

	public DestScores evaluateDestScores() {
		return null; // Placeholder for actual implementation
	}

	public List<TripMembership> getMemberships() {
		return null; // Placeholder for actual implementation
	}

}
