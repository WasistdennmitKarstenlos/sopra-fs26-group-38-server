package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.entity.Activity;
import ch.uzh.ifi.hase.soprafs26.entity.Comment;
import ch.uzh.ifi.hase.soprafs26.repository.ActivityRepository;
import ch.uzh.ifi.hase.soprafs26.repository.CommentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class CommentService {

    private static final int MAX_COMMENT_LENGTH = 280;

    private final CommentRepository commentRepository;
    private final ActivityRepository activityRepository;
    private final TripService tripService;

    public CommentService(CommentRepository commentRepository,
                          ActivityRepository activityRepository,
                          TripService tripService) {
        this.commentRepository = commentRepository;
        this.activityRepository = activityRepository;
        this.tripService = tripService;
    }

    public Comment addComment(Long tripId, Long destinationId, Long activityId, Long userId, String content) {
        tripService.getTripForParticipant(tripId, userId);

        Activity activity = activityRepository.findByIdAndTripIdAndDestinationId(activityId, tripId, destinationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Activity not found"));

        String normalizedContent = validateAndNormalizeContent(content);

        Comment comment = new Comment();
        comment.setTripId(tripId);
        comment.setDestinationId(destinationId);
        comment.setActivityId(activity.getId());
        comment.setUserId(userId);
        comment.setContent(normalizedContent);
        comment.setCreatedAt(LocalDateTime.now());

        return commentRepository.save(comment);
    }

    public List<Comment> getComments(Long tripId, Long destinationId, Long activityId, Long userId) {
        tripService.getTripForParticipant(tripId, userId);

        activityRepository.findByIdAndTripIdAndDestinationId(activityId, tripId, destinationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Activity not found"));

        return commentRepository.findByTripIdAndDestinationIdAndActivityIdOrderByCreatedAtAsc(tripId, destinationId, activityId);
    }

    public List<Comment> getCommentsForTrip(Long tripId, Long userId) {
        tripService.getTripForParticipant(tripId, userId);
        return commentRepository.findByTripIdOrderByCreatedAtAsc(tripId);
    }

    private String validateAndNormalizeContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Comment content cannot be empty");
        }

        String normalizedContent = content.trim();
        if (normalizedContent.length() > MAX_COMMENT_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Comment content cannot exceed 280 characters");
        }

        return normalizedContent;
    }
}