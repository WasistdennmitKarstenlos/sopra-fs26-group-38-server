package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.entity.Activity;
import ch.uzh.ifi.hase.soprafs26.entity.Trip;
import ch.uzh.ifi.hase.soprafs26.entity.Vote;
import ch.uzh.ifi.hase.soprafs26.entity.VoteType;
import ch.uzh.ifi.hase.soprafs26.repository.ActivityRepository;
import ch.uzh.ifi.hase.soprafs26.repository.VoteRepository;
import ch.uzh.ifi.hase.soprafs26.rest.dto.ActivitySearchResultDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.ActivityVoteRequestDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.ActivityVoteResponseDTO;
import ch.uzh.ifi.hase.soprafs26.event.DestinationVotesUpdatedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ActivityManagementService {

    private final ActivityRepository activityRepository;
    private final DestinationService destinationService;
    private final TripService tripService;
    private final VoteRepository voteRepository;
    private final ApplicationEventPublisher eventPublisher;

    public ActivityManagementService(ActivityRepository activityRepository,
                                     DestinationService destinationService,
                                     TripService tripService,
                                     VoteRepository voteRepository,
                                     ApplicationEventPublisher eventPublisher) {
        this.activityRepository = activityRepository;
        this.destinationService = destinationService;
        this.tripService = tripService;
        this.voteRepository = voteRepository;
        this.eventPublisher = eventPublisher;
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

    // Voting on an activity: users can upvote, downvote, or remove their vote. Each user can only have one vote per activity.
    public ActivityVoteResponseDTO voteOnActivity(Long activityId, Long userId, ActivityVoteRequestDTO voteRequest) {
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Activity not found"));
        return validateAndVote(activity.getTripId(), activity.getDestinationId(), activityId, userId, voteRequest);
    }

    // Helper method to validate and process the vote
    private ActivityVoteResponseDTO validateAndVote(Long tripId, Long destinationId, Long activityId, Long userId, ActivityVoteRequestDTO voteRequest) {
        if (voteRequest == null || voteRequest.getVoteType() == null || voteRequest.getVoteType().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "voteType is required");
        }

        // Validate and parse the vote type, ensuring it's either UP or DOWN (case-insensitive)
        VoteType voteType;
        try {
            voteType = VoteType.valueOf(voteRequest.getVoteType().trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid vote type. Use UP or DOWN.");
        }

        // Ensure the trip is active and the user is a participant before allowing voting
        Trip trip = tripService.getTripForParticipant(tripId, userId);
        if (trip.getStatus() != Trip.TripStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Voting is only allowed while the trip is ACTIVE");
        }

        destinationService.getDestinationEntity(tripId, destinationId);

        // Ensure the activity exists before allowing voting
        Activity activity = activityRepository.findByIdAndTripIdAndDestinationId(activityId, tripId, destinationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Activity not found"));

        // Check if the user has already voted on this activity. If so, update the vote; if not, create a new vote.
        Optional<Vote> existingVote = voteRepository.findByActivityIdAndUserId(activity.getId(), userId);
        if (existingVote.isEmpty()) {
            voteRepository.save(new Vote(activity.getId(), userId, voteType));
        } else {
            Vote vote = existingVote.get();
            if (vote.getVoteType() == voteType) {
                voteRepository.delete(vote);
            } else {
                vote.setVoteType(voteType);
                voteRepository.save(vote);
            }
        }

        eventPublisher.publishEvent(new DestinationVotesUpdatedEvent(this, tripId, userId));

        return buildVoteResponse(activity.getId(), userId);
    }

    // Helper method to build the vote response with current counts and user's vote
    private ActivityVoteResponseDTO buildVoteResponse(Long activityId, Long userId) {
        // Count the total upvotes and downvotes for the activity, and determine the user's current vote
        long upvotes = voteRepository.countByActivityIdAndVoteType(activityId, VoteType.UP);
        long downvotes = voteRepository.countByActivityIdAndVoteType(activityId, VoteType.DOWN);
        Optional<Vote> userVote = voteRepository.findByActivityIdAndUserId(activityId, userId);

        // Build and return the response DTO with the current vote counts and the user's vote
        ActivityVoteResponseDTO response = new ActivityVoteResponseDTO();
        response.setActivityId(activityId);
        response.setUpvotes(upvotes);
        response.setDownvotes(downvotes);
        response.setScore(upvotes - downvotes);
        response.setUserVote(userVote.map(vote -> vote.getVoteType().name()).orElse(null));
        return response;
    }

    // Helper method to get vote data for an activity (used when building ActivitySearchResultDTO)
    public void populateActivityVoteData(Activity activity, Long userId, ActivitySearchResultDTO dto) {
        long upvotes = voteRepository.countByActivityIdAndVoteType(activity.getId(), VoteType.UP);
        long downvotes = voteRepository.countByActivityIdAndVoteType(activity.getId(), VoteType.DOWN);
        Optional<Vote> userVote = voteRepository.findByActivityIdAndUserId(activity.getId(), userId);

        dto.setUpvotes(upvotes);
        dto.setDownvotes(downvotes);
        dto.setScore(upvotes - downvotes);
        dto.setUserVote(userVote.map(vote -> vote.getVoteType().name()).orElse(null));
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