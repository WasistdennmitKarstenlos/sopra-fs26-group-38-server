package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.entity.Trip;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.rest.dto.TripPostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.TripGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.InviteDTO;
import ch.uzh.ifi.hase.soprafs26.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs26.service.TripService;
import ch.uzh.ifi.hase.soprafs26.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

/**
 * TripController
 * This class is responsible for managing all Trip-related HTTP requests.
 * It handles creating trips, retrieving trip information, and updating trip status.
 */
@RestController
@RequestMapping("/trips")
public class TripController {

    private final TripService tripService;
    private final UserService userService;

    @Autowired
    public TripController(TripService tripService, UserService userService) {
        this.tripService = tripService;
        this.userService = userService;
    }

    /**
     * Get all trips
     * @return list of all trips
     */
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<TripGetDTO> getAllTrips() {
        List<Trip> trips = tripService.getAllTrips();
        return trips.stream()
                .map(DTOMapper.INSTANCE::convertEntityToTripGetDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get a specific trip by ID
     * @param tripId the ID of the trip
     * @return the trip details
     */
    @GetMapping("/{tripId}")
    @ResponseStatus(HttpStatus.OK)
    public TripGetDTO getTripById(@PathVariable Long tripId) {
        Trip trip = tripService.getTripById(tripId);
        return DTOMapper.INSTANCE.convertEntityToTripGetDTO(trip);
    }

    /**
     * Get a trip by room code
     * Used when users want to join a trip with the room code
     * @param roomCode the unique room code
     * @return the trip details
     */
    @GetMapping("/room/{roomCode}")
    @ResponseStatus(HttpStatus.OK)
    public TripGetDTO getTripByRoomCode(@PathVariable String roomCode) {
        Trip trip = tripService.getTripByRoomCode(roomCode);
        return DTOMapper.INSTANCE.convertEntityToTripGetDTO(trip);
    }

    /**
     * Get all trips hosted by a specific user
     * @param hostId the ID of the host user
     * @return list of trips hosted by the user
     */
    @GetMapping("/host/{hostId}")
    @ResponseStatus(HttpStatus.OK)
    public List<TripGetDTO> getTripsByHostId(@PathVariable Long hostId) {
        List<Trip> trips = tripService.getTripsByHostId(hostId);
        return trips.stream()
                .map(DTOMapper.INSTANCE::convertEntityToTripGetDTO)
                .collect(Collectors.toList());
    }

    /**
     * Create a new trip
     * The authenticated user becomes the host of the trip
     * @param tripPostDTO trip data with name
     * @param token Authorization header containing Bearer token
     * @return the created trip with room code
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TripGetDTO createTrip(@RequestBody TripPostDTO tripPostDTO,
                                 @RequestHeader(value = "Authorization", required = false) String token) {
        User authenticatedUser = userService.validateToken(token);

        Trip trip = DTOMapper.INSTANCE.convertTripPostDTOtoEntity(tripPostDTO);
        trip.setHostId(authenticatedUser.getId());
        
        Trip createdTrip = tripService.createTrip(trip);
        return DTOMapper.INSTANCE.convertEntityToTripGetDTO(createdTrip);
    }

    /**
     * Generate a shareable invite for a trip
     * Only the host can generate an invite
     * @param tripId the ID of the trip
     * @param token Authorization header containing Bearer token
     * @return the invite with room code
     * @throws ResponseStatusException 401 if user not authenticated
     * @throws ResponseStatusException 403 if user is not the host
     * @throws ResponseStatusException 404 if trip not found
     */
    @PostMapping("/{tripId}/invite")
    @ResponseStatus(HttpStatus.CREATED)
    public InviteDTO generateInvite(@PathVariable Long tripId,
                                    @RequestHeader(value = "Authorization", required = false) String token) {
        // Validate user is authenticated
        User authenticatedUser = userService.validateToken(token);

        // Get the trip - will throw 404 if not found
        Trip trip = tripService.getTripById(tripId);

        // Check if user is the host - throw 403 if not
        if (!trip.getHostId().equals(authenticatedUser.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only the host can generate an invite for this trip"
            );
        }

        // Return the invite with the room code
        return new InviteDTO(trip.getRoomCode());
    }

    /**
     * Update the status of a trip (e.g., to EVALUATION or FINALIZED)
     * @param tripId the ID of the trip
     * @param newStatus the new status
     * @return the updated trip
     */
    @PutMapping("/{tripId}/status")
    @ResponseStatus(HttpStatus.OK)
    public TripGetDTO updateTripStatus(@PathVariable Long tripId, 
                                        @RequestParam Trip.TripStatus newStatus) {
        Trip updatedTrip = tripService.updateTripStatus(tripId, newStatus);
        return DTOMapper.INSTANCE.convertEntityToTripGetDTO(updatedTrip);
    }

    /**
     * Set the final destination for a trip
     * Only the host can perform this action
     * @param tripId the ID of the trip
     * @param finalDestinationId the ID of the selected destination
     * @return the updated trip
     */
    @PutMapping("/{tripId}/finalize")
    @ResponseStatus(HttpStatus.OK)
    public TripGetDTO setFinalDestination(@PathVariable Long tripId,
                                           @RequestParam Long finalDestinationId) {
        Trip updatedTrip = tripService.setFinalDestination(tripId, finalDestinationId);
        return DTOMapper.INSTANCE.convertEntityToTripGetDTO(updatedTrip);
    }
}
