package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.entity.Activity;
import ch.uzh.ifi.hase.soprafs26.repository.ActivityRepository;
import ch.uzh.ifi.hase.soprafs26.rest.dto.ActivityPostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.ActivitySearchResultDTO;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@Transactional
public class ActivityManagementService {

    private final ActivityRepository activityRepository;
    private final DestinationService destinationService;

    public ActivityManagementService(ActivityRepository activityRepository,
                                     DestinationService destinationService) {
        this.activityRepository = activityRepository;
        this.destinationService = destinationService;
    }

    public List<ActivitySearchResultDTO> getSelectedActivities(Long tripId, Long destinationId) {
        destinationService.getDestinationEntity(tripId, destinationId);
        return activityRepository.findByTripIdAndDestinationIdOrderByIdDesc(tripId, destinationId)
                .stream()
                .map(this::toResultDTO)
                .toList();
    }

    public ActivitySearchResultDTO addActivity(Long tripId, Long destinationId, ActivityPostDTO activityPostDTO) {
        destinationService.getDestinationEntity(tripId, destinationId);
        validateActivityPostDTO(activityPostDTO);

        if (activityRepository.findByTripIdAndDestinationIdAndPlaceId(tripId, destinationId, activityPostDTO.getPlaceId()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Activity already added for this destination");
        }

        Activity activity = new Activity();
        activity.setTripId(tripId);
        activity.setDestinationId(destinationId);
        activity.setPlaceId(activityPostDTO.getPlaceId().trim());
        activity.setName(activityPostDTO.getName().trim());
        activity.setAddress(activityPostDTO.getAddress());
        activity.setRating(activityPostDTO.getRating());
        activity.setPhotoUrl(activityPostDTO.getPhotoUrl());
        activity.setLatitude(activityPostDTO.getLatitude());
        activity.setLongitude(activityPostDTO.getLongitude());

        Activity savedActivity = activityRepository.save(activity);
        return toResultDTO(savedActivity);
    }

    public ActivitySearchResultDTO updateActivity(Long tripId, Long destinationId, Long activityId, ActivityPostDTO activityPostDTO) {
        destinationService.getDestinationEntity(tripId, destinationId);
        validateActivityPostDTO(activityPostDTO);

        Activity activity = activityRepository.findByIdAndTripIdAndDestinationId(activityId, tripId, destinationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Activity not found"));

        activity.setPlaceId(activityPostDTO.getPlaceId().trim());
        activity.setName(activityPostDTO.getName().trim());
        activity.setAddress(activityPostDTO.getAddress());
        activity.setRating(activityPostDTO.getRating());
        activity.setPhotoUrl(activityPostDTO.getPhotoUrl());
        activity.setLatitude(activityPostDTO.getLatitude());
        activity.setLongitude(activityPostDTO.getLongitude());

        return toResultDTO(activityRepository.save(activity));
    }

    public void deleteActivity(Long tripId, Long destinationId, Long activityId) {
        destinationService.getDestinationEntity(tripId, destinationId);

        Activity activity = activityRepository.findByIdAndTripIdAndDestinationId(activityId, tripId, destinationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Activity not found"));

        activityRepository.delete(activity);
    }

    private void validateActivityPostDTO(ActivityPostDTO activityPostDTO) {
        if (activityPostDTO == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Activity payload is required");
        }
        if (activityPostDTO.getPlaceId() == null || activityPostDTO.getPlaceId().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "placeId cannot be empty");
        }
        if (activityPostDTO.getName() == null || activityPostDTO.getName().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name cannot be empty");
        }
    }

    private ActivitySearchResultDTO toResultDTO(Activity activity) {
        ActivitySearchResultDTO resultDTO = new ActivitySearchResultDTO();
        resultDTO.setId(activity.getId());
        resultDTO.setPlaceId(activity.getPlaceId());
        resultDTO.setName(activity.getName());
        resultDTO.setAddress(activity.getAddress());
        resultDTO.setRating(activity.getRating());
        resultDTO.setPhotoUrl(activity.getPhotoUrl());
        resultDTO.setLatitude(activity.getLatitude());
        resultDTO.setLongitude(activity.getLongitude());
        return resultDTO;
    }
}