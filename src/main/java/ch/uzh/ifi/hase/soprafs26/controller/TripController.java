package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.entity.Trip;
import ch.uzh.ifi.hase.soprafs26.rest.dto.TripPostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.TripGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs26.service.TripService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

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

    @Autowired
    public TripController(TripService tripService) {
        this.tripService = tripService;
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
     * @param hostId the ID of the user creating the trip (should come from auth context in a real app)
     * @return the created trip with room code
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TripGetDTO createTrip(@RequestBody TripPostDTO tripPostDTO, 
                                  @RequestParam Long hostId) {
        Trip trip = DTOMapper.INSTANCE.convertTripPostDTOtoEntity(tripPostDTO);
        trip.setHostId(hostId);
        
        Trip createdTrip = tripService.createTrip(trip);
        return DTOMapper.INSTANCE.convertEntityToTripGetDTO(createdTrip);
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

    /**
     * Add a destination proposal to a trip.
     * Only authenticated trip participants can add destinations.
     * @param tripId target trip id
     * @param destinationPostDTO request body containing locationName
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

}
