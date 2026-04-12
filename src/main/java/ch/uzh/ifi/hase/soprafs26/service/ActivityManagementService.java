package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.entity.Activity;
import ch.uzh.ifi.hase.soprafs26.repository.ActivityRepository;
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
    private final TripService tripService;

    public ActivityManagementService(ActivityRepository activityRepository,
                                     DestinationService destinationService,
                                     TripService tripService) {
        this.activityRepository = activityRepository;
        this.destinationService = destinationService;
        this.tripService = tripService;
    }

    public List<Activity> getSelectedActivities(Long tripId, Long destinationId) {
        destinationService.getDestinationEntity(tripId, destinationId);
        return activityRepository.findByTripIdAndDestinationIdOrderByIdDesc(tripId, destinationId)
                .stream()
                .toList();
    }

    public Activity addActivity(Long tripId, Long destinationId, Activity activityInput) {
        tripService.ensureTripIsActiveForMutations(tripId);
        destinationService.getDestinationEntity(tripId, destinationId);
        validateActivity(activityInput);

        if (activityRepository.findByTripIdAndDestinationIdAndPlaceId(tripId, destinationId, activityInput.getPlaceId()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Activity already added for this destination");
        }

        Activity activity = new Activity();
        activity.setTripId(tripId);
        activity.setDestinationId(destinationId);
        activity.setPlaceId(activityInput.getPlaceId().trim());
        activity.setName(activityInput.getName().trim());
        activity.setAddress(activityInput.getAddress());
        activity.setRating(activityInput.getRating());
        activity.setPhotoUrl(activityInput.getPhotoUrl());
        activity.setLatitude(activityInput.getLatitude());
        activity.setLongitude(activityInput.getLongitude());

        return activityRepository.save(activity);
    }

    public Activity updateActivity(Long tripId, Long destinationId, Long activityId, Activity activityUpdate) {
        tripService.ensureTripIsActiveForMutations(tripId);
        destinationService.getDestinationEntity(tripId, destinationId);
        validateActivity(activityUpdate);

        Activity activity = activityRepository.findByIdAndTripIdAndDestinationId(activityId, tripId, destinationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Activity not found"));

        activity.setPlaceId(activityUpdate.getPlaceId().trim());
        activity.setName(activityUpdate.getName().trim());
        activity.setAddress(activityUpdate.getAddress());
        activity.setRating(activityUpdate.getRating());
        activity.setPhotoUrl(activityUpdate.getPhotoUrl());
        activity.setLatitude(activityUpdate.getLatitude());
        activity.setLongitude(activityUpdate.getLongitude());

        return activityRepository.save(activity);
    }

    public void deleteActivity(Long tripId, Long destinationId, Long activityId) {
        tripService.ensureTripIsActiveForMutations(tripId);
        destinationService.getDestinationEntity(tripId, destinationId);

        Activity activity = activityRepository.findByIdAndTripIdAndDestinationId(activityId, tripId, destinationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Activity not found"));

        activityRepository.delete(activity);
    }

    private void validateActivity(Activity activity) {
        if (activity == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Activity payload is required");
        }
        if (activity.getPlaceId() == null || activity.getPlaceId().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "placeId cannot be empty");
        }
        if (activity.getName() == null || activity.getName().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name cannot be empty");
        }
    }
}