package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.entity.Activity;
import ch.uzh.ifi.hase.soprafs26.entity.Destination;
import ch.uzh.ifi.hase.soprafs26.entity.Vote;
import ch.uzh.ifi.hase.soprafs26.entity.VoteType;
import ch.uzh.ifi.hase.soprafs26.repository.ActivityRepository;
import ch.uzh.ifi.hase.soprafs26.repository.DestinationRepository;
import ch.uzh.ifi.hase.soprafs26.repository.VoteRepository;
import ch.uzh.ifi.hase.soprafs26.rest.dto.DestinationGetDTO;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@Transactional
public class DestinationService {

    private final DestinationRepository destinationRepository;
    private final ActivityRepository activityRepository;
    private final VoteRepository voteRepository;
    private final TripService tripService;

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

        return destinationRepository.save(destination);
    }

    public Destination updateDestination(Long tripId, Long destinationId, Destination destinationUpdate) {
        tripService.ensureTripIsActiveForMutations(tripId);
        validate(destinationUpdate);
        Destination destination = getDestinationEntity(tripId, destinationId);
        destination.setDestinationName(destinationUpdate.getDestinationName().trim());
        return destinationRepository.save(destination);
    }

    public void deleteDestination(Long tripId, Long destinationId) {
        tripService.ensureTripIsActiveForMutations(tripId);
        Destination destination = getDestinationEntity(tripId, destinationId);
        destinationRepository.delete(destination);
    }

    public Destination getDestinationEntity(Long tripId, Long destinationId) {
        tripService.getTripById(tripId);
        return destinationRepository.findByIdAndTripId(destinationId, tripId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Destination not found"));
    }

    public void populateDestinationVoteData(Destination destination, Long userId, DestinationGetDTO dto) {
        List<Activity> activities = activityRepository.findByTripIdAndDestinationIdOrderByIdDesc(
                destination.getTripId(), destination.getId());

        if (activities.isEmpty()) {
            dto.setUpvotes(0L);
            dto.setDownvotes(0L);
            dto.setScore(0L);
            dto.setUserVote(null);
            return;
        }

        List<Long> activityIds = activities.stream().map(Activity::getId).toList();
        List<Vote> votes = voteRepository.findByActivityIdIn(activityIds);

        long upvotes = votes.stream().filter(v -> v.getVoteType() == VoteType.UP).count();
        long downvotes = votes.stream().filter(v -> v.getVoteType() == VoteType.DOWN).count();
        long totalVotes = upvotes + downvotes;
        long score = totalVotes == 0 ? 0 : Math.round((upvotes - downvotes) * ((double) upvotes / totalVotes));

        dto.setUpvotes(upvotes);
        dto.setDownvotes(downvotes);
        dto.setScore(score);
        dto.setUserVote(null);
    }

    private void validate(Destination destination) {
        if (destination == null || destination.getDestinationName() == null || destination.getDestinationName().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Destination name cannot be empty");
        }
    }
}