package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.entity.Destination;
import ch.uzh.ifi.hase.soprafs26.entity.Trip;
import ch.uzh.ifi.hase.soprafs26.repository.DestinationRepository;
import ch.uzh.ifi.hase.soprafs26.repository.TripRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class TripService {

    private final Logger log = LoggerFactory.getLogger(TripService.class);
    private final TripRepository tripRepository;
    private final DestinationRepository destinationRepository;
    @Autowired

    public TripService(
            TripRepository tripRepository,
            DestinationRepository destinationRepository,
        this.tripRepository = tripRepository;
        this.destinationRepository = destinationRepository;
    }

    /**
     * Get all trips in the system
     * @return list of all trips
     */
    public List<Trip> getAllTrips() {
        log.debug("Fetching all trips");
        return tripRepository.findAll();
    }

    /**
     * Get a trip by its ID
     * @param tripId the ID of the trip
     * @return the trip if found
     * @throws ResponseStatusException if trip not found
     */
    public Trip getTripById(Long tripId) {
        log.debug("Fetching trip with id: {}", tripId);
        return tripRepository.findById(tripId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Trip with id " + tripId + " not found"
                ));
    }

    /**
     * Get a trip by its room code
     * @param roomCode the unique room code
     * @return the trip if found
     * @throws ResponseStatusException if room code not found
     */
    public Trip getTripByRoomCode(String roomCode) {
        log.debug("Fetching trip with room code: {}", roomCode);
        return tripRepository.findByRoomCode(roomCode)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Trip with room code " + roomCode + " not found"
                ));
    }

    /**
     * Get all trips hosted by a specific user
     * @param hostId the ID of the host user
     * @return list of trips hosted by the user
     */
    public List<Trip> getTripsByHostId(Long hostId) {
        log.debug("Fetching trips hosted by user: {}", hostId);
        return tripRepository.findByHostId(hostId);
    }

    /**
     * Create a new trip
     * @param trip the trip to create (must have name and hostId set)
     * @return the created trip with room code and ID
     * @throws ResponseStatusException if validation fails
     */
    public Trip createTrip(Trip trip) {
        log.debug("Creating new trip with name: {} for host: {}", trip.getName(), trip.getHostId());

        // Validate input
        validateTripCreation(trip);

        // Generate unique room code
        String roomCode = generateUniqueRoomCode();
        trip.setRoomCode(roomCode);

        // Save to database
        Trip savedTrip = tripRepository.save(trip);
        log.info("Trip created successfully with id: {}, room code: {}", savedTrip.getId(), roomCode);

        return savedTrip;
    }
    /**
     * Add destination proposal to a trip for a participant.
     * @param tripId target trip id
     * @param userId authenticated user id
     * @param destination destination input
     * @return created destination
     */
    public Destination addDestination(Long tripId, Long userId, Destination destination) {
        Trip trip = getTripById(tripId);
        validateDestinationInput(destination);
        ensureParticipant(tripId, userId);

        if (trip.getStatus() != Trip.TripStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Destinations can only be added while trip is ACTIVE");
        }

        destination.setTripId(tripId);
        destination.setProposedByUserId(userId);

        return destinationRepository.save(destination);
    }

    /**
     * Get shared destinations of a trip for participants.
     * @param tripId target trip id
     * @param userId authenticated user id
     * @return ordered list of destinations
     */
    public List<Destination> getDestinations(Long tripId, Long userId) {
        getTripById(tripId);
        ensureParticipant(tripId, userId);
        return destinationRepository.findByTripIdOrderByCreatedAtAsc(tripId);
    }

    /**
     * Update trip status (e.g., to EVALUATION or FINALIZED)
     * @param tripId the ID of the trip
     * @param newStatus the new status
     * @return the updated trip
     * @throws ResponseStatusException if trip not found or invalid status transition
     */
    public Trip updateTripStatus(Long tripId, Trip.TripStatus newStatus) {
        log.debug("Updating trip {} status to: {}", tripId, newStatus);

        Trip trip = getTripById(tripId);
        trip.setStatus(newStatus);

        Trip updatedTrip = tripRepository.save(trip);
        log.info("Trip {} status updated to: {}", tripId, newStatus);

        return updatedTrip;
    }

    /**
     * Set the final destination for a trip
     * @param tripId the ID of the trip
     * @param finalDestinationId the ID of the destination selected as final
     * @return the updated trip
     * @throws ResponseStatusException if trip not found
     */
    public Trip setFinalDestination(Long tripId, Long finalDestinationId) {
        log.debug("Setting final destination {} for trip {}", finalDestinationId, tripId);

        Trip trip = getTripById(tripId);
        trip.setFinalDestinationId(finalDestinationId);
        trip.setStatus(Trip.TripStatus.FINALIZED);

        Trip updatedTrip = tripRepository.save(trip);
        log.info("Final destination set for trip {}", tripId);

        return updatedTrip;
    }

    /**
     * Validate trip creation input
     * @param trip the trip to validate
     * @throws ResponseStatusException if validation fails
     */
    private void validateTripCreation(Trip trip) {
        // Check if trip name is provided
        if (trip.getName() == null || trip.getName().trim().isEmpty()) {
            log.warn("Trip creation failed: trip name is empty");
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Trip name cannot be empty"
            );
        }

        // Check if host ID is provided
        if (trip.getHostId() == null) {
            log.warn("Trip creation failed: host ID is null");
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Host ID is required"
            );
        }

        // Optional: Check if user already has a trip with same name
        if (tripRepository.findByNameAndHostId(trip.getName(), trip.getHostId()).isPresent()) {
            log.warn("Trip creation failed: user {} already has trip named {}", trip.getHostId(), trip.getName());
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "User already has a trip with this name"
            );
        }
    }

    /**
     * Generate a unique room code for a trip
     * @return a unique 6-character room code
     */
    private String generateUniqueRoomCode() {
        String roomCode;
        int attempts = 0;
        final int MAX_ATTEMPTS = 10;

        do {
            // Generate a 6-character alphanumeric code
            roomCode = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
            attempts++;
        } while (tripRepository.findByRoomCode(roomCode).isPresent() && attempts < MAX_ATTEMPTS);

        if (attempts >= MAX_ATTEMPTS) {
            log.error("Failed to generate unique room code after {} attempts", MAX_ATTEMPTS);
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to generate unique room code"
            );
        }

        return roomCode;
    }

    private void ensureParticipant(Long tripId, Long userId) {
        if (!tripMembershipRepository.existsByTripIdAndUserId(tripId, userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User is not a participant of this trip");
        }
    }

    private void validateDestinationInput(Destination destination) {
        if (destination.getLocationName() == null || destination.getLocationName().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Location name cannot be empty");
        }
        destination.setLocationName(destination.getLocationName().trim());
    }
}
