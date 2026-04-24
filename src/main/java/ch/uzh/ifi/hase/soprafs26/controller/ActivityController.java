package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.entity.Activity;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.rest.dto.ActivityPostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.ActivitySearchResultDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.ActivityVoteRequestDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.ActivityVoteResponseDTO;
import ch.uzh.ifi.hase.soprafs26.service.ActivityManagementService;
import ch.uzh.ifi.hase.soprafs26.service.ActivitySearchService;
import ch.uzh.ifi.hase.soprafs26.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.DeleteMapping;

import java.util.List;

@RestController
public class ActivityController {

    private final ActivitySearchService activitySearchService;
    private final ActivityManagementService activityManagementService;
    private final UserService userService;

    public ActivityController(ActivitySearchService activitySearchService,
                              ActivityManagementService activityManagementService,
                              UserService userService) {
        this.activitySearchService = activitySearchService;
        this.activityManagementService = activityManagementService;
        this.userService = userService;
    }

    @GetMapping("/trips/{tripId}/destinations/{destinationId}/activities")
    @ResponseStatus(HttpStatus.OK)
    public List<ActivitySearchResultDTO> searchActivities(@PathVariable("tripId") Long tripId,
                                                          @PathVariable("destinationId") Long destinationId,
                                                          @RequestParam(value = "query", required = false) String query,
                                                          @RequestParam(value = "location", required = false) String location,
                                                          @RequestParam(value = "radius", required = false) Integer radius,
                                                          @RequestHeader(value = "Authorization", required = false) String token) {
        User authenticatedUser = userService.validateToken(token);

        if (query == null || query.trim().isEmpty()) {
            return activityManagementService.getSelectedActivities(tripId, destinationId).stream()
                    .map(activity -> toSearchResultDTO(activity, authenticatedUser.getId(), activityManagementService))
                    .toList();
        }

        return activitySearchService.searchActivities(tripId, destinationId, query, location, radius);
    }

    @PostMapping("/trips/{tripId}/destinations/{destinationId}/activities")
    @ResponseStatus(HttpStatus.CREATED)
    public ActivitySearchResultDTO addActivity(@PathVariable("tripId") Long tripId,
                                               @PathVariable("destinationId") Long destinationId,
                                               @RequestBody ActivityPostDTO activityPostDTO,
                                               @RequestHeader(value = "Authorization", required = false) String token) {
        User authenticatedUser = userService.validateToken(token);
        Activity activity = toActivityEntity(activityPostDTO);
        Activity saved = activityManagementService.addActivity(tripId, destinationId, authenticatedUser.getId(), activity);
        return toSearchResultDTO(saved, authenticatedUser.getId(), activityManagementService);
    }

    @PutMapping("/trips/{tripId}/destinations/{destinationId}/activities/{activityId}")
    @ResponseStatus(HttpStatus.OK)
    public ActivitySearchResultDTO updateActivity(@PathVariable("tripId") Long tripId,
                                                  @PathVariable("destinationId") Long destinationId,
                                                  @PathVariable("activityId") Long activityId,
                                                  @RequestBody ActivityPostDTO activityPostDTO,
                                                  @RequestHeader(value = "Authorization", required = false) String token) {
        User authenticatedUser = userService.validateToken(token);
        Activity update = toActivityEntity(activityPostDTO);
        Activity saved = activityManagementService.updateActivity(tripId, destinationId, activityId, authenticatedUser.getId(), update);
        return toSearchResultDTO(saved, authenticatedUser.getId(), activityManagementService);
    }

    @PutMapping("/activities/{activityId}/vote")
    @ResponseStatus(HttpStatus.OK)
    public ActivityVoteResponseDTO voteActivity(@PathVariable("activityId") Long activityId,
                                                @RequestBody ActivityVoteRequestDTO voteRequest,
                                                @RequestHeader(value = "Authorization", required = false) String token) {
        var requester = userService.validateToken(token);
        return activityManagementService.voteOnActivity(activityId, requester.getId(), voteRequest);
    }

    @DeleteMapping("/trips/{tripId}/destinations/{destinationId}/activities/{activityId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteActivity(@PathVariable("tripId") Long tripId,
                               @PathVariable("destinationId") Long destinationId,
                               @PathVariable("activityId") Long activityId,
                               @RequestHeader(value = "Authorization", required = false) String token) {
        User requester = userService.validateToken(token);
        activityManagementService.deleteActivity(tripId, destinationId, activityId, requester.getId());
    }

    @GetMapping("/activities/search")
    @ResponseStatus(HttpStatus.OK)
    public List<ActivitySearchResultDTO> searchActivitiesLegacy(@RequestParam("query") String query,
                                                                @RequestParam(value = "location", required = false) String location,
                                                                @RequestParam(value = "radius", required = false) Integer radius,
                                                                @RequestHeader(value = "Authorization", required = false) String token) {
        userService.validateToken(token);
        return activitySearchService.searchActivities(null, null, query, location, radius);
    }

    private static Activity toActivityEntity(ActivityPostDTO activityPostDTO) {
        // Prefer MapStruct if mappings are added later.
        Activity activity = new Activity();
        if (activityPostDTO == null) {
            return activity;
        }
        activity.setPlaceId(activityPostDTO.getPlaceId());
        activity.setName(activityPostDTO.getName());
        activity.setAddress(activityPostDTO.getAddress());
        activity.setRating(activityPostDTO.getRating());
        activity.setPhotoUrl(activityPostDTO.getPhotoUrl());
        activity.setLatitude(activityPostDTO.getLatitude());
        activity.setLongitude(activityPostDTO.getLongitude());
        return activity;
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
        
        // Populate vote data if user is authenticated
        if (userId != null) {
            activityManagementService.populateActivityVoteData(activity, userId, resultDTO);
        }
        
        return resultDTO;
    }
}