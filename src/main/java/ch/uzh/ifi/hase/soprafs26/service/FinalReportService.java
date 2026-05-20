package ch.uzh.ifi.hase.soprafs26.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.entity.Activity;
import ch.uzh.ifi.hase.soprafs26.entity.Comment;
import ch.uzh.ifi.hase.soprafs26.entity.Destination;
import ch.uzh.ifi.hase.soprafs26.entity.Trip;
import ch.uzh.ifi.hase.soprafs26.entity.Vote;
import ch.uzh.ifi.hase.soprafs26.entity.VoteType;
import ch.uzh.ifi.hase.soprafs26.repository.ActivityRepository;
import ch.uzh.ifi.hase.soprafs26.repository.CommentRepository;
import ch.uzh.ifi.hase.soprafs26.repository.VoteRepository;
import ch.uzh.ifi.hase.soprafs26.rest.dto.DestinationGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.FinalReportGetDTO;

@Service
@Transactional(readOnly = true)
public class FinalReportService {

    private final TripService tripService;
    private final DestinationService destinationService;
    private final ActivityRepository activityRepository;
    private final VoteRepository voteRepository;
    private final CommentRepository commentRepository;

    public FinalReportService(TripService tripService,
                              DestinationService destinationService,
                              ActivityRepository activityRepository,
                              VoteRepository voteRepository,
                              CommentRepository commentRepository) {
        this.tripService = tripService;
        this.destinationService = destinationService;
        this.activityRepository = activityRepository;
        this.voteRepository = voteRepository;
        this.commentRepository = commentRepository;
    }

    public FinalReportGetDTO getFinalReport(Long tripId, Long requesterId) {
        Trip trip = tripService.getTripForParticipant(tripId, requesterId);

        if (trip.getStatus() != Trip.TripStatus.FINALIZED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Final report is only available for finalized trips"
            );
        }

        if (trip.getFinalDestinationId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Final report is unavailable because no final destination has been selected"
            );
        }

        Destination winningDestination = destinationService.getDestinationEntity(tripId, trip.getFinalDestinationId());
        List<Activity> activities = activityRepository.findByTripIdAndDestinationIdOrderByIdDesc(tripId, winningDestination.getId());

        FinalReportGetDTO report = new FinalReportGetDTO();
        report.setTripId(trip.getId());
        report.setTripName(trip.getName());
        report.setRoomCode(trip.getRoomCode());
        report.setGeneratedAt(new Date());

        FinalReportGetDTO.WinningDestinationDTO destinationDTO = new FinalReportGetDTO.WinningDestinationDTO();
        destinationDTO.setId(winningDestination.getId());
        destinationDTO.setName(winningDestination.getDestinationName());

        Map<Long, VoteTally> voteTallies = buildVoteTallies(activities);
        List<FinalReportGetDTO.ActivityFinalOutcomeDTO> activityOutcomes = new ArrayList<>();

        for (Activity activity : activities) {
            VoteTally tally = voteTallies.getOrDefault(activity.getId(), new VoteTally(0L, 0L));

            FinalReportGetDTO.ActivityFinalOutcomeDTO activityDTO = new FinalReportGetDTO.ActivityFinalOutcomeDTO();
            activityDTO.setId(activity.getId());
            activityDTO.setPlaceId(activity.getPlaceId());
            activityDTO.setName(activity.getName());
            activityDTO.setAddress(activity.getAddress());
            activityDTO.setRating(activity.getRating());
            activityDTO.setUpvotes(tally.upvotes());
            activityDTO.setDownvotes(tally.downvotes());
            activityDTO.setScore(tally.upvotes() - tally.downvotes());
            
            // Fetch comments for this activity
            List<Comment> activityComments = commentRepository.findByTripIdAndDestinationIdAndActivityIdOrderByCreatedAtAsc(
                tripId,
                winningDestination.getId(),
                activity.getId()
            );
            List<String> commentTexts = activityComments.stream()
                    .map(Comment::getContent)
                    .toList();
            activityDTO.setComments(commentTexts);

            activityOutcomes.add(activityDTO);
        }

        activityOutcomes.sort(
            Comparator.comparingLong(FinalReportGetDTO.ActivityFinalOutcomeDTO::getScore).reversed()
                .thenComparing(Comparator.comparingLong(FinalReportGetDTO.ActivityFinalOutcomeDTO::getUpvotes).reversed())
                .thenComparing(FinalReportGetDTO.ActivityFinalOutcomeDTO::getName, Comparator.nullsLast(String::compareToIgnoreCase))
        );

        for (int index = 0; index < activityOutcomes.size(); index++) {
            activityOutcomes.get(index).setRank(index + 1);
        }

        long totalUpvotes = activityOutcomes.stream().mapToLong(FinalReportGetDTO.ActivityFinalOutcomeDTO::getUpvotes).sum();
        long totalDownvotes = activityOutcomes.stream().mapToLong(FinalReportGetDTO.ActivityFinalOutcomeDTO::getDownvotes).sum();

        DestinationGetDTO scoreDTO = new DestinationGetDTO();
        destinationService.populateDestinationVoteData(winningDestination, requesterId, scoreDTO);
        double destinationScore = scoreDTO.getScore() == null ? 0.0 : scoreDTO.getScore();

        destinationDTO.setTotalUpvotes(totalUpvotes);
        destinationDTO.setTotalDownvotes(totalDownvotes);
        destinationDTO.setTotalScore(destinationScore);
        destinationDTO.setActivities(activityOutcomes);

        report.setWinningDestination(destinationDTO);
        return report;
    }

    private Map<Long, VoteTally> buildVoteTallies(List<Activity> activities) {
        Map<Long, VoteTally> tallies = new HashMap<>();
        if (activities.isEmpty()) {
            return tallies;
        }

        List<Long> activityIds = activities.stream().map(Activity::getId).toList();
        List<Vote> votes = voteRepository.findByActivityIdIn(activityIds);

        for (Vote vote : votes) {
            VoteTally current = tallies.getOrDefault(vote.getActivityId(), new VoteTally(0L, 0L));
            if (vote.getVoteType() == VoteType.UP) {
                tallies.put(vote.getActivityId(), new VoteTally(current.upvotes() + 1, current.downvotes()));
            } else {
                tallies.put(vote.getActivityId(), new VoteTally(current.upvotes(), current.downvotes() + 1));
            }
        }

        return tallies;
    }

    private record VoteTally(long upvotes, long downvotes) {
    }
}