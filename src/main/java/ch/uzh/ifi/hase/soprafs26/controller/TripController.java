package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.entity.Destination;
import ch.uzh.ifi.hase.soprafs26.entity.Trip;
import ch.uzh.ifi.hase.soprafs26.rest.dto.DestinationGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.DestinationPostDTO;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.rest.dto.TripPostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.TripGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.InviteDTO;
import ch.uzh.ifi.hase.soprafs26.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs26.service.DestinationRealtimeService;
import ch.uzh.ifi.hase.soprafs26.service.TripService;
import ch.uzh.ifi.hase.soprafs26.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
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
    private final DestinationRealtimeService destinationRealtimeService;

    public TripController(
            TripService tripService,
            UserService userService,
            DestinationRealtimeService destinationRealtimeService) {
        this.tripService = tripService;
        this.userService = userService;
        this.destinationRealtimeService = destinationRealtimeService;
    }

    /**
     * Get all trips
     * @return list of all trips
     */
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<TripGetDTO> getAllTrips(@RequestHeader(value = "Authorization", required = false) String token) {
        userService.validateToken(token);
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
    public TripGetDTO getTripById(@RequestHeader(value = "Authorization", required = false) String token,@PathVariable Long tripId) {
        userService.validateToken(token);
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
    public TripGetDTO getTripByRoomCode(@RequestHeader(value = "Authorization", required = false) String token,
                                        @PathVariable String roomCode) {
        userService.validateToken(token);
        Trip trip = tripService.getTripByRoomCode(roomCode);
        return DTOMapper.INSTANCE.convertEntityToTripGetDTO(trip);
    }

    /**
     * Join a trip using a room code.
     * Requires authentication and records membership for destination permissions.
     * @param roomCode room code to join
     * @param token authorization header
     * @return joined trip details
     */
    @PostMapping("/join/{roomCode}")
    @ResponseStatus(HttpStatus.OK)
    public TripGetDTO joinTripByRoomCode(
            @PathVariable String roomCode,
            @RequestHeader(value = "Authorization", required = false) String token) {
        User requester = userService.validateToken(token);
        Trip trip = tripService.joinTripByRoomCode(roomCode, requester.getId());
        return DTOMapper.INSTANCE.convertEntityToTripGetDTO(trip);
    }

    /**
     * Get all trips hosted by a specific user
     * @param hostId the ID of the host user
     * @return list of trips hosted by the user
     */
    @GetMapping("/host/{hostId}")
    @ResponseStatus(HttpStatus.OK)
    public List<TripGetDTO> getTripsByHostId(@RequestHeader(value = "Authorization", required = false) String token,
                                             @PathVariable Long hostId) {
        userService.validateToken(token);
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
    public TripGetDTO updateTripStatus(@RequestHeader(value = "Authorization", required = false) String token,
                                       @PathVariable Long tripId,
                                       @RequestParam Trip.TripStatus newStatus) {
        userService.validateToken(token);
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
    public TripGetDTO setFinalDestination(@RequestHeader(value = "Authorization", required = false) String token,
                                           @PathVariable Long tripId,
                                           @RequestParam Long finalDestinationId) {
        userService.validateToken(token);
        Trip updatedTrip = tripService.setFinalDestination(tripId, finalDestinationId);
        return DTOMapper.INSTANCE.convertEntityToTripGetDTO(updatedTrip);
    }

    /**
     * Add a destination proposal to a trip.
     * Only authenticated trip participants can add destinations.
     * @param tripId target trip id
     * @param destinationPostDTO request body containing destination name
     * @param token authorization header
     * @return created destination
     */
    @PostMapping("/{tripId}/destinations")
    @ResponseStatus(HttpStatus.CREATED)
    public DestinationGetDTO addDestination(
            @PathVariable Long tripId,
            @RequestBody DestinationPostDTO destinationPostDTO,
            @RequestHeader(value = "Authorization", required = false) String token) {
        User requester = userService.validateToken(token);
        Destination destination = DTOMapper.INSTANCE.convertDestinationPostDTOtoEntity(destinationPostDTO);
        Destination savedDestination = tripService.addDestination(tripId, requester.getId(), destination);

        if (destinationRealtimeService != null) {
            List<DestinationGetDTO> sharedList = tripService.getDestinations(tripId, requester.getId()).stream()
                    .map(DTOMapper.INSTANCE::convertEntityToDestinationGetDTO)
                    .collect(Collectors.toList());
            destinationRealtimeService.publish(tripId, sharedList);
        }

        return DTOMapper.INSTANCE.convertEntityToDestinationGetDTO(savedDestination);
    }

    /**
     * Get shared destination proposals for a trip.
     * Only authenticated trip participants can see the list.
     * @param tripId target trip id
     * @param token authorization header
     * @return destination list
     */
    @GetMapping("/{tripId}/destinations")
    @ResponseStatus(HttpStatus.OK)
    public List<DestinationGetDTO> getDestinations(
            @PathVariable Long tripId,
            @RequestHeader(value = "Authorization", required = false) String token) {
        User requester = userService.validateToken(token);
        return tripService.getDestinations(tripId, requester.getId()).stream()
                .map(DTOMapper.INSTANCE::convertEntityToDestinationGetDTO)
                .collect(Collectors.toList());
    }

    /**
     * Real-time stream for destination list updates in a trip.
     * Clients subscribe once and receive updates on each saved destination.
     * @param tripId target trip id
     * @param token authorization header
     * @return SSE emitter
     */
    @GetMapping(value = "/{tripId}/destinations/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public SseEmitter streamDestinations(
            @PathVariable Long tripId,
            @RequestHeader(value = "Authorization", required = false) String token) {
        User requester = userService.validateToken(token);
        tripService.getDestinations(tripId, requester.getId());
        return destinationRealtimeService.subscribe(tripId);
    }
}
