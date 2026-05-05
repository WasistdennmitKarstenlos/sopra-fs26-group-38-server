package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.entity.Activity;
import ch.uzh.ifi.hase.soprafs26.entity.Comment;
import ch.uzh.ifi.hase.soprafs26.entity.Trip;
import ch.uzh.ifi.hase.soprafs26.repository.ActivityRepository;
import ch.uzh.ifi.hase.soprafs26.repository.CommentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private ActivityRepository activityRepository;

    @Mock
    private TripService tripService;

    @InjectMocks
    private CommentService commentService;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void addComment_validInput_success() {
        Trip trip = new Trip();
        trip.setId(1L);

        Activity activity = new Activity();
        activity.setId(10L);
        activity.setTripId(1L);
        activity.setDestinationId(2L);

        Mockito.when(tripService.getTripForParticipant(1L, 100L)).thenReturn(trip);
        Mockito.when(activityRepository.findByIdAndTripIdAndDestinationId(10L, 1L, 2L)).thenReturn(Optional.of(activity));
        Mockito.when(commentRepository.save(Mockito.any(Comment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Comment result = commentService.addComment(1L, 2L, 10L, 100L, "  Nice place  ");

        assertEquals(1L, result.getTripId());
        assertEquals(2L, result.getDestinationId());
        assertEquals(10L, result.getActivityId());
        assertEquals(100L, result.getUserId());
        assertEquals("Nice place", result.getContent());
        assertNotNull(result.getCreatedAt());
    }

    @Test
    public void addComment_emptyContent_throwsBadRequest() {
        Trip trip = new Trip();
        Activity activity = new Activity();
        activity.setId(10L);

        Mockito.when(tripService.getTripForParticipant(1L, 100L)).thenReturn(trip);
        Mockito.when(activityRepository.findByIdAndTripIdAndDestinationId(10L, 1L, 2L)).thenReturn(Optional.of(activity));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> commentService.addComment(1L, 2L, 10L, 100L, "   "));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    public void addComment_tooLong_throwsBadRequest() {
        Trip trip = new Trip();
        Activity activity = new Activity();
        activity.setId(10L);

        Mockito.when(tripService.getTripForParticipant(1L, 100L)).thenReturn(trip);
        Mockito.when(activityRepository.findByIdAndTripIdAndDestinationId(10L, 1L, 2L)).thenReturn(Optional.of(activity));

        String longComment = "a".repeat(281);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> commentService.addComment(1L, 2L, 10L, 100L, longComment));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    public void getComments_success() {
        Trip trip = new Trip();
        trip.setId(1L);

        Activity activity = new Activity();
        activity.setId(10L);
        activity.setTripId(1L);
        activity.setDestinationId(2L);

        Comment comment = new Comment();
        comment.setId(5L);
        comment.setTripId(1L);
        comment.setDestinationId(2L);
        comment.setActivityId(10L);
        comment.setUserId(100L);
        comment.setContent("Hello");

        Mockito.when(tripService.getTripForParticipant(1L, 100L)).thenReturn(trip);
        Mockito.when(activityRepository.findByIdAndTripIdAndDestinationId(10L, 1L, 2L)).thenReturn(Optional.of(activity));
        Mockito.when(commentRepository.findByTripIdAndDestinationIdAndActivityIdOrderByCreatedAtAsc(1L, 2L, 10L))
                .thenReturn(List.of(comment));

        List<Comment> result = commentService.getComments(1L, 2L, 10L, 100L);

        assertEquals(1, result.size());
        assertEquals("Hello", result.get(0).getContent());
        Mockito.verify(commentRepository, Mockito.times(1))
                .findByTripIdAndDestinationIdAndActivityIdOrderByCreatedAtAsc(1L, 2L, 10L);
    }
}