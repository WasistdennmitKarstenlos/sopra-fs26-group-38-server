package ch.uzh.ifi.hase.soprafs26.service;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.entity.Activity;
import ch.uzh.ifi.hase.soprafs26.entity.Destination;
import ch.uzh.ifi.hase.soprafs26.entity.Vote;
import ch.uzh.ifi.hase.soprafs26.entity.VoteType;
import ch.uzh.ifi.hase.soprafs26.repository.ActivityRepository;
import ch.uzh.ifi.hase.soprafs26.repository.DestinationRepository;
import ch.uzh.ifi.hase.soprafs26.repository.VoteRepository;
import ch.uzh.ifi.hase.soprafs26.rest.dto.DestinationGetDTO;

@Service
@Transactional
public class DestinationService {

    private final DestinationRepository destinationRepository;
    private final ActivityRepository activityRepository;
    private final VoteRepository voteRepository;
    private final TripService tripService;

    // Wilson Score confidence interval constant (95% confidence level)
    private static final double WILSON_Z_SCORE = 1.96;

    // Weight for destination-level Wilson score in combined ranking (70%)
    private static final double DESTINATION_WEIGHT = 0.7;

    // Weight for top activity score average in combined ranking (30%)
    private static final double TOP_ACTIVITIES_WEIGHT = 0.3;

    // Number of highest-ranked activity scores to include in the average
    private static final int TOP_ACTIVITIES_COUNT = 3;

    public DestinationService(DestinationRepository destinationRepository,
                              ActivityRepository activityRepository,
                              VoteRepository voteRepository,
                              TripService tripService) {
        this.destinationRepository = destinationRepository;
        this.activityRepository = activityRepository;
        this.voteRepository = voteRepository;
        this.tripService = tripService;
    }

    public List<Destination> getDestinations(Long tripId) {
        tripService.getTripById(tripId);
        return destinationRepository.findByTripIdOrderByIdDesc(tripId);
    }

    public Destination createDestination(Long tripId, Destination destination) {
        tripService.ensureTripIsActiveForMutations(tripId);
        destination.setTripId(tripId);
        validate(destination);
        ensureDestinationNameIsUnique(tripId, destination.getDestinationName());

        return destinationRepository.save(destination);
    }

    public Destination updateDestination(Long tripId, Long destinationId, Destination destinationUpdate, Long requesterId) {
        tripService.ensureTripIsActiveForMutations(tripId);
        validate(destinationUpdate);

        Destination destination = getDestinationEntity(tripId, destinationId);
        ensureDestinationNameIsUniqueForUpdate(tripId, destinationUpdate.getDestinationName(), destinationId);

        Long proposedBy = destination.getProposedByUserId();
        if (proposedBy == null || !proposedBy.equals(requesterId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the creator can edit this destination");
        }

        // prevent editing if destination already has activities
        boolean hasActivities = !activityRepository.findByTripIdAndDestinationIdOrderByIdDesc(tripId, destinationId).isEmpty();
        if (hasActivities) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Destination already has activities and cannot be edited");
        }

        destination.setDestinationName(destinationUpdate.getDestinationName().trim());
        return destinationRepository.save(destination);
    }

    public void deleteDestination(Long tripId, Long destinationId, Long requesterId) {
        tripService.ensureTripIsActiveForMutations(tripId);
        Destination destination = getDestinationEntity(tripId, destinationId);

        // Validation 1: Only creator can delete
        Long proposedBy = destination.getProposedByUserId();
        if (proposedBy == null || !proposedBy.equals(requesterId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the creator can delete this destination");
        }

        // Validation 2: Destination must not have activities
        List<Activity> activities = activityRepository.findByTripIdAndDestinationIdOrderByIdDesc(tripId, destinationId);
        if (!activities.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Destination has associated activities and cannot be deleted");
        }

        destinationRepository.delete(destination);
    }

    public Destination getDestinationEntity(Long tripId, Long destinationId) {
        tripService.getTripById(tripId);
        return destinationRepository.findByIdAndTripId(destinationId, tripId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Destination not found"));
    }

    /**
     * Populates destination vote data using a hybrid Wilson Score ranking algorithm.
     *
     * The final score is calculated as a weighted combination of:
     * - 70% destination-level Wilson score (all activity votes combined)
     * - 30% average of top activity Wilson scores
     *
     * Wilson Score provides statistically robust confidence intervals and helps prevent
     * overrating items with very low vote counts.
     *
     * @param destination the destination entity with associated activities
     * @param userId the user ID (currently unused, retained for future extensibility)
     * @param dto the DestinationGetDTO to populate with upvotes, downvotes, and score
     */
    public void populateDestinationVoteData(Destination destination, Long userId, DestinationGetDTO dto) {
        List<Activity> activities = activityRepository.findByTripIdAndDestinationIdOrderByIdDesc(
                destination.getTripId(), destination.getId());

        if (activities.isEmpty()) {
            dto.setUpvotes(0L);
            dto.setDownvotes(0L);
            dto.setScore(0.0);
            dto.setUserVote(null);
            return;
        }

        List<Long> activityIds = activities.stream().map(Activity::getId).toList();
        List<Vote> votes = voteRepository.findByActivityIdIn(activityIds);

        long totalUpvotes = votes.stream()
            .filter(v -> v.getVoteType() == VoteType.UP)
            .count();

        long totalDownvotes = votes.stream()
            .filter(v -> v.getVoteType() == VoteType.DOWN)
            .count();

        Map<Long, List<Vote>> votesByActivity = votes.stream()
            .collect(Collectors.groupingBy(Vote::getActivityId));

        List<Double> topActivityScores = activities.stream()
            .map(activity -> {
                List<Vote> activityVotes = votesByActivity.getOrDefault(
                    activity.getId(), Collections.emptyList());

                long upvotes = activityVotes.stream()
                    .filter(v -> v.getVoteType() == VoteType.UP)
                    .count();

                long downvotes = activityVotes.stream()
                    .filter(v -> v.getVoteType() == VoteType.DOWN)
                    .count();

                return wilsonScore(upvotes, downvotes);
            })
            .sorted(Comparator.reverseOrder())
            .limit(TOP_ACTIVITIES_COUNT)
            .toList();

        double topActivityAverage = topActivityScores.stream()
            .mapToDouble(Double::doubleValue)
            .average()
            .orElse(0.0);

        double destinationWilson = wilsonScore(totalUpvotes, totalDownvotes);
        double finalScore = DESTINATION_WEIGHT * destinationWilson
            + TOP_ACTIVITIES_WEIGHT * topActivityAverage;

        dto.setUpvotes(totalUpvotes);
        dto.setDownvotes(totalDownvotes);
        dto.setScore(Math.round(finalScore * 10.0 * 10.0) / 10.0);
        dto.setUserVote(null);
    }

        /**
         * Calculates the lower bound of the Wilson Score confidence interval.
         *
         * @param upvotes number of positive votes
         * @param downvotes number of negative votes
         * @return a Wilson score in [0, 1], or 0 if there are no votes
         */
        private double wilsonScore(long upvotes, long downvotes) {
        long n = upvotes + downvotes;
        if (n == 0) {
            return 0.0;
        }

        double p = (double) upvotes / n;
        double zSquared = WILSON_Z_SCORE * WILSON_Z_SCORE;
        double denominator = 1 + (zSquared / n);
        double center = p + (zSquared / (2.0 * n));
        double margin = WILSON_Z_SCORE * Math.sqrt((p * (1 - p) + (zSquared / (4.0 * n))) / n);

        return (center - margin) / denominator;
        }

        /**
         * Test helper that exposes the Wilson score calculation for unit tests.
         *
         * @param upvotes number of positive votes
         * @param downvotes number of negative votes
         * @return a Wilson score in [0, 1]
         */
        public double testWilsonScore(long upvotes, long downvotes) {
        return wilsonScore(upvotes, downvotes);
        }

    private void validate(Destination destination) {
        if (destination == null || destination.getDestinationName() == null || destination.getDestinationName().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Destination name cannot be empty");
        }
        destination.setDestinationName(destination.getDestinationName().trim());
    }

    private void ensureDestinationNameIsUnique(Long tripId, String destinationName) {
        if (destinationRepository.existsByTripIdAndDestinationNameIgnoreCase(tripId, destinationName)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Destination with this name already exists for this trip");
        }
    }

    private void ensureDestinationNameIsUniqueForUpdate(Long tripId, String destinationName, Long currentDestinationId) {
        if (destinationRepository.existsByTripIdAndDestinationNameIgnoreCaseAndIdNot(tripId, destinationName, currentDestinationId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Destination with this name already exists for this trip");
        }
    }
}
