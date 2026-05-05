package ch.uzh.ifi.hase.soprafs26.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.entity.Trip;
import ch.uzh.ifi.hase.soprafs26.entity.TripMembership;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.rest.dto.InviteDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.JoinTripRequestDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.JoinTripResponseDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.FinalReportGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.TripGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.TripParticipantDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.TripPostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs26.service.DestinationRealtimeService;
import ch.uzh.ifi.hase.soprafs26.service.FinalReportService;
import ch.uzh.ifi.hase.soprafs26.service.TripService;
import ch.uzh.ifi.hase.soprafs26.service.UserService;

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
    private final FinalReportService finalReportService;

    public TripController(
            TripService tripService,
            UserService userService,
            DestinationRealtimeService destinationRealtimeService,
            FinalReportService finalReportService) {
        this.tripService = tripService;
        this.userService = userService;
        this.destinationRealtimeService = destinationRealtimeService;
        this.finalReportService = finalReportService;
    }

    /**
     * Get all trips
     * @return list of all trips
     */
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<TripGetDTO> getAllTrips(@RequestHeader(value = "Authorization", required = false) String token) {
        User authenticatedUser = userService.validateToken(token);
        List<Trip> trips = tripService.getTripsForUser(authenticatedUser.getId());
        return trips.stream()
                .map(trip -> toTripGetDTOForUser(trip, authenticatedUser.getId()))
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
        User authenticatedUser = userService.validateToken(token);
        Trip trip = tripService.getTripById(tripId);
        return toTripGetDTOForUser(trip, authenticatedUser.getId());
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
        User authenticatedUser = userService.validateToken(token);
        Trip trip = tripService.getTripByRoomCode(roomCode);
        return toTripGetDTOForUser(trip, authenticatedUser.getId());
    }

    /**
     * Join a trip using a room code.
     * Requires authentication and records membership for destination permissions.
     * @param requestDTO request body containing roomCode
     * @param token authorization header
     * @return joined trip details
     */
    @PostMapping("/join")
    @ResponseStatus(HttpStatus.OK)
    public JoinTripResponseDTO joinTrip(
            @RequestBody JoinTripRequestDTO requestDTO,
            @RequestHeader(value = "Authorization", required = false) String token) {
        User requester = userService.validateToken(token);
        Trip trip = tripService.joinTripByRoomCode(requestDTO.getRoomCode(), requester.getId());

        JoinTripResponseDTO response = new JoinTripResponseDTO();
        response.setTripId(trip.getId());
        response.setRoomCode(trip.getRoomCode());
        response.setUserId(requester.getId());
        return response;
    }

    /**
     * Get all participants of a trip.
     * @param tripId trip id
     * @param token authorization header
     * @return list of participants with their account username
     */
    @GetMapping("/{tripId}/participants")
    @ResponseStatus(HttpStatus.OK)
    public List<TripParticipantDTO> getTripParticipants(
            @PathVariable Long tripId,
            @RequestHeader(value = "Authorization", required = false) String token) {
        User requester = userService.validateToken(token);
        List<TripMembership> memberships = tripService.getTripParticipants(tripId, requester.getId());

        return memberships.stream().map(membership -> {
            TripParticipantDTO participant = new TripParticipantDTO();
            participant.setUserId(membership.getUserId());
            User memberUser = userService.getUserById(membership.getUserId());
            participant.setUsername(memberUser.getUsername());
            return participant;
        }).collect(Collectors.toList());
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
        User authenticatedUser = userService.validateToken(token);
        List<Trip> trips = tripService.getTripsByHostId(hostId);
        return trips.stream()
            .map(trip -> toTripGetDTOForUser(trip, authenticatedUser.getId()))
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
        return toTripGetDTOForUser(createdTrip, authenticatedUser.getId());
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
        User authenticatedUser = userService.validateToken(token);

        if (newStatus != Trip.TripStatus.EVALUATION) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only transition to EVALUATION is supported via this endpoint"
            );
        }

        Trip updatedTrip = tripService.enterFinalEvaluation(tripId, authenticatedUser.getId());
        return toTripGetDTOForUser(updatedTrip, authenticatedUser.getId());
    }

    /**
     * Enter final evaluation mode.
     * Dedicated endpoint for host action "Final Evaluation".
     * @param tripId the ID of the trip
     * @param token Authorization header containing Bearer token
     * @return the updated trip in EVALUATION status
     */
    @PostMapping("/{tripId}/final-evaluation")
    @ResponseStatus(HttpStatus.OK)
    public TripGetDTO enterFinalEvaluation(@RequestHeader(value = "Authorization", required = false) String token,
                                           @PathVariable Long tripId) {
        User authenticatedUser = userService.validateToken(token);
        Trip updatedTrip = tripService.enterFinalEvaluation(tripId, authenticatedUser.getId());
        return toTripGetDTOForUser(updatedTrip, authenticatedUser.getId());
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
        User authenticatedUser = userService.validateToken(token);
        Trip updatedTrip = tripService.setFinalDestination(tripId, finalDestinationId);
        return toTripGetDTOForUser(updatedTrip, authenticatedUser.getId());
    }

    /**
     * Fetch the compact final report for a finalized trip.
     * Access is limited to authenticated participants of the trip.
     * @param token Authorization header containing Bearer token
     * @param tripId the ID of the trip
     * @return compact final report data
     */
    @GetMapping("/{tripId}/final-report")
    @ResponseStatus(HttpStatus.OK)
    public FinalReportGetDTO getFinalReport(@RequestHeader(value = "Authorization", required = false) String token,
                                            @PathVariable("tripId") Long tripId) {
        User authenticatedUser = userService.validateToken(token);
        return finalReportService.getFinalReport(tripId, authenticatedUser.getId());
    }

    private TripGetDTO toTripGetDTOForUser(Trip trip, Long userId) {
        TripGetDTO dto = DTOMapper.INSTANCE.convertEntityToTripGetDTO(trip);
        boolean isHost = trip.getHostId() != null && trip.getHostId().equals(userId);
        dto.setHost(isHost);
        dto.setEvaluationMode(trip.getStatus() == Trip.TripStatus.EVALUATION);
        dto.setFinalized(trip.getStatus() == Trip.TripStatus.FINALIZED);
        dto.setCanEnterFinalEvaluation(tripService.canEnterFinalEvaluation(trip, userId));
        return dto;
    }
}
