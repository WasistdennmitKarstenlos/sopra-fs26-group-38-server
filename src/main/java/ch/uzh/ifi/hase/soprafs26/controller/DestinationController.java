package ch.uzh.ifi.hase.soprafs26.controller;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import ch.uzh.ifi.hase.soprafs26.entity.Activity;
import ch.uzh.ifi.hase.soprafs26.entity.Destination;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.event.DestinationVotesUpdatedEvent;
import ch.uzh.ifi.hase.soprafs26.event.TripStatusUpdatedEvent;
import ch.uzh.ifi.hase.soprafs26.rest.dto.ActivitySearchResultDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.DestinationGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.DestinationPostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs26.service.ActivityManagementService;
import ch.uzh.ifi.hase.soprafs26.service.DestinationRealtimeService;
import ch.uzh.ifi.hase.soprafs26.service.DestinationService;
import ch.uzh.ifi.hase.soprafs26.service.TripService;
import ch.uzh.ifi.hase.soprafs26.service.UserService;
import jakarta.servlet.http.HttpServletResponse;

@RestController
public class DestinationController {

    private final DestinationService destinationService;
    private final TripService tripService;
    private final ActivityManagementService activityManagementService;
    private final UserService userService;
    private final DestinationRealtimeService destinationRealtimeService;

    public DestinationController(
            DestinationService destinationService,
            TripService tripService,
            ActivityManagementService activityManagementService,
            UserService userService,
            DestinationRealtimeService destinationRealtimeService) {
        this.destinationService = destinationService;
        this.tripService = tripService;
        this.activityManagementService = activityManagementService;
        this.userService = userService;
        this.destinationRealtimeService = destinationRealtimeService;
    }

    @GetMapping("/trips/{tripId}/destinations")
    @ResponseStatus(HttpStatus.OK)
    public List<DestinationGetDTO> getDestinations(@PathVariable("tripId") Long tripId,
                                                   @RequestHeader(value = "Authorization", required = false) String token) {
        User requester = userService.validateToken(token);
        return buildSharedDestinationList(tripId, requester.getId());
    }

    @PostMapping("/trips/{tripId}/destinations")
    @ResponseStatus(HttpStatus.CREATED)
    public DestinationGetDTO createDestination(@PathVariable("tripId") Long tripId,
                                               @RequestBody DestinationPostDTO destinationPostDTO,
                                               @RequestHeader(value = "Authorization", required = false) String token) {
        User requester = userService.validateToken(token);
        Destination destination = DTOMapper.INSTANCE.convertDestinationPostDTOtoEntity(destinationPostDTO);
        Destination savedDestination = tripService.addDestination(tripId, requester.getId(), destination);

        if (destinationRealtimeService != null) {
            List<DestinationGetDTO> sharedList = buildSharedDestinationList(tripId, requester.getId());
            destinationRealtimeService.publish(tripId, sharedList);
        }

        DestinationGetDTO dto = DTOMapper.INSTANCE.convertEntityToDestinationGetDTO(savedDestination);
        dto.setActivities(buildActivities(tripId, savedDestination.getId(), requester.getId()));
        destinationService.populateDestinationVoteData(savedDestination, requester.getId(), dto);
        return dto;
    }

    /**
     * Real-time stream for destination list updates in a trip.
     * Clients subscribe once and receive updates on each saved destination.
     * @param tripId target trip id
     * @param token authorization header
     * @return SSE emitter
     */
    @GetMapping(value = "/trips/{tripId}/destinations/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamDestinations(
            @PathVariable Long tripId,
            @RequestHeader(value = "Authorization", required = false) String token,
            HttpServletResponse response) {
        User requester = userService.validateToken(token);
        tripService.getDestinations(tripId, requester.getId());

        response.setContentType(MediaType.TEXT_EVENT_STREAM_VALUE);
        response.setHeader("Cache-Control", "no-cache, no-transform");
        response.setHeader("X-Accel-Buffering", "no");

        return destinationRealtimeService.subscribe(tripId);
    }

    @EventListener
    public void handleDestinationVotesUpdated(DestinationVotesUpdatedEvent event) {
        if (destinationRealtimeService != null) {
            List<DestinationGetDTO> sharedList = buildSharedDestinationList(event.getTripId(), event.getUserId());
            destinationRealtimeService.publish(event.getTripId(), sharedList);
        }
    }

    @EventListener
    public void handleTripStatusUpdated(TripStatusUpdatedEvent event) {
        if (destinationRealtimeService != null) {
            destinationRealtimeService.publish(event.getTripId(), "trip-status-updated", event.getStatus());
        }
    }

    @PutMapping("/trips/{tripId}/destinations/{destinationId}")
    @ResponseStatus(HttpStatus.OK)
    public DestinationGetDTO updateDestination(@PathVariable("tripId") Long tripId,
                                               @PathVariable("destinationId") Long destinationId,
                                               @RequestBody DestinationPostDTO destinationPostDTO,
                                               @RequestHeader(value = "Authorization", required = false) String token) {
        User requester = userService.validateToken(token);
        Destination update = DTOMapper.INSTANCE.convertDestinationPostDTOtoEntity(destinationPostDTO);
        Destination saved = destinationService.updateDestination(tripId, destinationId, update, requester.getId());

        if (destinationRealtimeService != null) {
            List<DestinationGetDTO> sharedList = buildSharedDestinationList(tripId, requester.getId());
            destinationRealtimeService.publish(tripId, sharedList);
        }

        DestinationGetDTO dto = DTOMapper.INSTANCE.convertEntityToDestinationGetDTO(saved);
        dto.setActivities(buildActivities(tripId, saved.getId(), requester.getId()));
        destinationService.populateDestinationVoteData(saved, requester.getId(), dto);
        return dto;
    }

    @DeleteMapping("/trips/{tripId}/destinations/{destinationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDestination(@PathVariable("tripId") Long tripId,
                                  @PathVariable("destinationId") Long destinationId,
                                  @RequestHeader(value = "Authorization", required = false) String token) {
        userService.validateToken(token);
        destinationService.deleteDestination(tripId, destinationId);
    }

    private List<DestinationGetDTO> buildSharedDestinationList(Long tripId, Long requesterId) {
        return tripService.getDestinations(tripId, requesterId).stream()
                .filter(Objects::nonNull)
                .map(destination -> {
                    DestinationGetDTO dto = DTOMapper.INSTANCE.convertEntityToDestinationGetDTO(destination);
                    dto.setActivities(buildActivities(tripId, destination.getId(), requesterId));
                    destinationService.populateDestinationVoteData(destination, requesterId, dto);
                    return dto;
                })
                .collect(Collectors.toList());
    }

    private List<ActivitySearchResultDTO> buildActivities(Long tripId, Long destinationId, Long userId) {
        return activityManagementService.getSelectedActivities(tripId, destinationId).stream()
                .map(activity -> toSearchResultDTO(activity, userId, activityManagementService))
                .toList();
    }

    private static ActivitySearchResultDTO toSearchResultDTO(Activity activity, Long userId, ActivityManagementService activityManagementService) {
        ActivitySearchResultDTO resultDTO = new ActivitySearchResultDTO();
        if (activity == null) {
            return resultDTO;
        }
        resultDTO.setId(activity.getId());
        resultDTO.setPlaceId(activity.getPlaceId());
        resultDTO.setName(activity.getName());
        resultDTO.setAddress(activity.getAddress());
        resultDTO.setRating(activity.getRating());
        resultDTO.setPhotoUrl(activity.getPhotoUrl());
        resultDTO.setLatitude(activity.getLatitude());
        resultDTO.setLongitude(activity.getLongitude());
        resultDTO.setCreatedBy(activity.getCreatedBy());
        
        // Populate vote data if user is authenticated
        if (userId != null) {
            activityManagementService.populateActivityVoteData(activity, userId, resultDTO);
        }
        
        return resultDTO;
    }
}
