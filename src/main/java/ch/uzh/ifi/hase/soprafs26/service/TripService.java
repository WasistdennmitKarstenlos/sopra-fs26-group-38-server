package ch.uzh.ifi.hase.soprafs26.service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.entity.Destination;
import ch.uzh.ifi.hase.soprafs26.entity.Trip;
import ch.uzh.ifi.hase.soprafs26.entity.TripMembership;
import ch.uzh.ifi.hase.soprafs26.repository.DestinationRepository;
import ch.uzh.ifi.hase.soprafs26.repository.TripMembershipRepository;
import ch.uzh.ifi.hase.soprafs26.repository.TripRepository;

@Service
@Transactional
public class TripService {

    private final Logger log = LoggerFactory.getLogger(TripService.class);
    private final TripRepository tripRepository;
    private final DestinationRepository destinationRepository;
    private final TripMembershipRepository tripMembershipRepository;

    public TripService(
            TripRepository tripRepository,
            DestinationRepository destinationRepository,
            TripMembershipRepository tripMembershipRepository) {
        this.tripRepository = tripRepository;
        this.destinationRepository = destinationRepository;
        this.tripMembershipRepository = tripMembershipRepository;
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
     * Get all trips that the given user created or joined.
     * @param userId the authenticated user's ID
     * @return trips ordered by creation date descending
     */
    public List<Trip> getTripsForUser(Long userId) {
        log.debug("Fetching trips for user: {}", userId);
        List<Long> tripIds = tripMembershipRepository.findByUserId(userId)
                .stream()
                .map(TripMembership::getTripId)
                .collect(Collectors.toList());
        return tripRepository.findByIdInOrderByCreationDateDesc(tripIds);
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
        tripMembershipRepository.save(new TripMembership(savedTrip.getId(), savedTrip.getHostId()));
        log.info("Trip created successfully with id: {}, room code: {}", savedTrip.getId(), roomCode);

        return savedTrip;
    }

    /**
     * Join a trip by room code.
     * @param roomCode room code for the trip
     * @param userId logged-in user id
     * @param roomUsername username for this user within the trip room
     * @return the joined trip
     */
    public Trip joinTripByRoomCode(String roomCode, Long userId, String roomUsername) {
        validateJoinTrip(roomCode, roomUsername);
        Trip trip = getTripByRoomCode(roomCode);
        String normalizedRoomUsername = roomUsername.trim();

        if (trip.getStatus() != Trip.TripStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Trip is closed for joining");
        }

        if (tripMembershipRepository.existsByTripIdAndUserId(trip.getId(), userId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User is already a member of this trip");
        }

        if (tripMembershipRepository.existsByTripIdAndRoomUsername(trip.getId(), normalizedRoomUsername)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Room username is already taken in this trip");
        }

        tripMembershipRepository.save(new TripMembership(trip.getId(), userId, normalizedRoomUsername));

        return trip;
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
        ensureTripIsActiveForMutations(trip);

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
        return destinationRepository.findByTripIdOrderByIdDesc(tripId);
    }

    // validates that the user is a participant of the trip and returns the trip
    public Trip getTripForParticipant(Long tripId, Long userId) {
        Trip trip = getTripById(tripId);
        ensureParticipant(tripId, userId);
        return trip;
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
        validateEvaluationTransition(trip, newStatus);
        trip.setStatus(newStatus);

        Trip updatedTrip = tripRepository.save(trip);
        log.info("Trip {} status updated to: {}", tripId, newStatus);

        return updatedTrip;
    }

    /**
     * Shared guard for write operations that are only allowed while the trip is ACTIVE.
     * Can be reused by other services handling destination/activity/comment/vote mutations.
     * @param trip trip entity
     */
    public void ensureTripIsActiveForMutations(Trip trip) {
        if (trip.getStatus() != Trip.TripStatus.ACTIVE) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Trip is in read-only mode. Mutations are only allowed while trip is ACTIVE"
            );
        }
    }

    /**
     * Convenience overload to enforce ACTIVE mode by trip id.
     * @param tripId target trip id
     */
    public void ensureTripIsActiveForMutations(Long tripId) {
        ensureTripIsActiveForMutations(getTripById(tripId));
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
     * Validate join trip input
     * @param roomCode the room code to join
     * @param  roomUsername the username for this user within the trip room
     * @throws ResponseStatusException if validation fails
     */
    private void validateJoinTrip(String roomCode, String roomUsername) {
        if (roomCode == null || roomCode.trim().isEmpty()) {
            log.warn("Join trip failed: room code is empty");
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Room code cannot be empty"
            );
        }
        if (roomUsername == null || roomUsername.trim().isEmpty() || roomUsername.length() > 20) {
            log.warn("Join trip failed: room username is empty or too long");
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Room username cannot be empty or exceed 20 characters"
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
        if (destination.getDestinationName() == null || destination.getDestinationName().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Destination name cannot be empty");
        }
        destination.setDestinationName(destination.getDestinationName().trim());
    }

    /**
     * Validates whether the requested status transition is allowed for entering evaluation mode.
     */
    private void validateEvaluationTransition(Trip trip, Trip.TripStatus newStatus) {
        if (newStatus != Trip.TripStatus.EVALUATION) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only transition to EVALUATION is supported via this endpoint"
            );
        }

        if (trip.getStatus() == Trip.TripStatus.EVALUATION) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Trip is already in EVALUATION mode");
        }

        if (trip.getStatus() == Trip.TripStatus.FINALIZED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Trip is FINALIZED and cannot enter EVALUATION");
        }

        if (destinationRepository.findByTripIdOrderByIdDesc(trip.getId()).isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "At least one destination is required before entering EVALUATION"
            );
        }
    }
}
