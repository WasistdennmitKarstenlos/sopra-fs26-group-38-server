package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.entity.Activity;
import ch.uzh.ifi.hase.soprafs26.entity.Destination;
import ch.uzh.ifi.hase.soprafs26.entity.Trip;
import ch.uzh.ifi.hase.soprafs26.repository.ActivityRepository;
import ch.uzh.ifi.hase.soprafs26.repository.VoteRepository;
import ch.uzh.ifi.hase.soprafs26.rest.dto.ActivityCommentRequestDTO;
import org.springframework.context.ApplicationEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ActivityManagementServiceTest {

    @Mock
    private ActivityRepository activityRepository;

    @Mock
    private DestinationService destinationService;

    @Mock
    private TripService tripService;

    @Mock
    private VoteRepository voteRepository;
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ActivityManagementService activityManagementService;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void addActivity_validInput_success() {
        Activity input = new Activity();
        input.setPlaceId("place-1");
        input.setName("City Museum");
        input.setAddress("Main Street 1");
        input.setRating(4.5);
        input.setPhotoUrl("https://example.com/photo.jpg");
        input.setComment("Great for rainy days");

        Activity saved = new Activity();
        saved.setId(1L);
        saved.setTripId(1L);
        saved.setDestinationId(2L);
        saved.setPlaceId("place-1");
        saved.setName("City Museum");
        saved.setAddress("Main Street 1");
        saved.setRating(4.5);
        saved.setPhotoUrl("https://example.com/photo.jpg");
        saved.setComment("Great for rainy days");

        Destination destination = new Destination();
        destination.setId(2L);
        destination.setTripId(1L);

        Mockito.when(destinationService.getDestinationEntity(1L, 2L)).thenReturn(destination);
        Mockito.when(activityRepository.findByTripIdAndDestinationIdAndPlaceId(1L, 2L, "place-1"))
                .thenReturn(Optional.empty());
        Mockito.when(activityRepository.save(Mockito.any(Activity.class))).thenReturn(saved);

        Activity result = activityManagementService.addActivity(1L, 2L, 100L, input);

        assertEquals("place-1", result.getPlaceId());
        assertEquals("City Museum", result.getName());
        assertEquals("https://example.com/photo.jpg", result.getPhotoUrl());
        assertEquals("Great for rainy days", result.getComment());
    }

    @Test
    public void addActivity_duplicate_throwsConflict() {
        Activity input = new Activity();
        input.setPlaceId("place-1");
        input.setName("City Museum");

        Destination destination = new Destination();
        destination.setId(2L);
        destination.setTripId(1L);

        Mockito.when(destinationService.getDestinationEntity(1L, 2L)).thenReturn(destination);
        Mockito.when(activityRepository.findByTripIdAndDestinationIdAndPlaceId(1L, 2L, "place-1"))
                .thenReturn(Optional.of(new Activity()));

        assertThrows(ResponseStatusException.class, () -> activityManagementService.addActivity(1L, 2L, 100L, input));
    }

    @Test
    public void getSelectedActivities_success() {
        Activity activity = new Activity();
        activity.setTripId(1L);
        activity.setDestinationId(2L);
        activity.setPlaceId("place-1");
        activity.setName("City Museum");

        Destination destination = new Destination();
        destination.setId(2L);
        destination.setTripId(1L);

        Mockito.when(destinationService.getDestinationEntity(1L, 2L)).thenReturn(destination);
        Mockito.when(activityRepository.findByTripIdAndDestinationIdOrderByIdDesc(1L, 2L))
                .thenReturn(List.of(activity));

        List<Activity> result = activityManagementService.getSelectedActivities(1L, 2L);

        assertEquals(1, result.size());
        assertEquals("place-1", result.get(0).getPlaceId());
        assertEquals("City Museum", result.get(0).getName());
    }

    @Test
    public void updateActivity_success() {
        Activity input = new Activity();
        input.setPlaceId("place-1");
        input.setName("City Museum Updated");
        input.setComment("  Better than expected  ");

        Activity existing = new Activity();
        existing.setId(10L);
        existing.setTripId(1L);
        existing.setDestinationId(2L);
        existing.setPlaceId("place-1");
        existing.setName("Old");
        existing.setComment("old comment");

        Destination destination = new Destination();
        destination.setId(2L);
        destination.setTripId(1L);

        Mockito.when(destinationService.getDestinationEntity(1L, 2L)).thenReturn(destination);
        Mockito.when(activityRepository.findByIdAndTripIdAndDestinationId(10L, 1L, 2L)).thenReturn(Optional.of(existing));
        Mockito.when(activityRepository.save(Mockito.any(Activity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Activity result = activityManagementService.updateActivity(1L, 2L, 10L, 100L, input);
        assertEquals("City Museum Updated", result.getName());
        assertEquals("Better than expected", result.getComment());
    }

    @Test
    public void updateActivityComment_success() {
        Activity existing = new Activity();
        existing.setId(10L);
        existing.setTripId(1L);
        existing.setDestinationId(2L);
        existing.setComment("old");

        Trip trip = new Trip("Trip", "ROOM1", 1L);
        trip.setId(1L);
        trip.setStatus(Trip.TripStatus.ACTIVE);

        Destination destination = new Destination();
        destination.setId(2L);
        destination.setTripId(1L);

        ActivityCommentRequestDTO request = new ActivityCommentRequestDTO();
        request.setComment("  New comment from user  ");

        Mockito.when(activityRepository.findById(10L)).thenReturn(Optional.of(existing));
        Mockito.when(tripService.getTripForParticipant(1L, 5L)).thenReturn(trip);
        Mockito.when(destinationService.getDestinationEntity(1L, 2L)).thenReturn(destination);
        Mockito.when(activityRepository.save(Mockito.any(Activity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Activity updated = activityManagementService.updateActivityComment(10L, 5L, request);

        assertEquals(10L, updated.getId());
        assertEquals("New comment from user", updated.getComment());
    }

    @Test
    public void addActivity_commentTooLong_throwsBadRequest() {
        Activity input = new Activity();
        input.setPlaceId("place-1");
        input.setName("City Museum");
        input.setComment("x".repeat(281));

        Destination destination = new Destination();
        destination.setId(2L);
        destination.setTripId(1L);

        Mockito.when(destinationService.getDestinationEntity(1L, 2L)).thenReturn(destination);
        Mockito.when(activityRepository.findByTripIdAndDestinationIdAndPlaceId(1L, 2L, "place-1"))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> activityManagementService.addActivity(1L, 2L, input)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    public void deleteActivity_success() {
        Activity existing = new Activity();
        existing.setId(10L);
        existing.setTripId(1L);
        existing.setDestinationId(2L);

        Destination destination = new Destination();
        destination.setId(2L);
        destination.setTripId(1L);

        Mockito.when(destinationService.getDestinationEntity(1L, 2L)).thenReturn(destination);
        Mockito.when(activityRepository.findByIdAndTripIdAndDestinationId(10L, 1L, 2L)).thenReturn(Optional.of(existing));

        activityManagementService.deleteActivity(1L, 2L, 10L, 100L);

        Mockito.verify(activityRepository, Mockito.times(1)).delete(existing);
    }

    @Test
    public void addActivity_readOnlyMode_badRequest() {
        Activity input = new Activity();
        input.setPlaceId("place-1");
        input.setName("City Museum");

        Mockito.doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "read-only"))
                .when(tripService).ensureTripIsActiveForMutations(1L);

        assertThrows(ResponseStatusException.class, () -> activityManagementService.addActivity(1L, 2L, 100L, input));
        Mockito.verifyNoInteractions(destinationService, activityRepository);
    }

    @Test
    public void updateActivity_readOnlyMode_badRequest() {
        Activity input = new Activity();
        input.setPlaceId("place-1");
        input.setName("City Museum Updated");

        Mockito.doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "read-only"))
                .when(tripService).ensureTripIsActiveForMutations(1L);

        assertThrows(ResponseStatusException.class, () -> activityManagementService.updateActivity(1L, 2L, 10L, 100L, input));
        Mockito.verifyNoInteractions(destinationService, activityRepository);
    }

    @Test
    public void deleteActivity_readOnlyMode_badRequest() {
        Mockito.doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "read-only"))
                .when(tripService).ensureTripIsActiveForMutations(1L);

        assertThrows(ResponseStatusException.class, () -> activityManagementService.deleteActivity(1L, 2L, 10L, 100L));
        Mockito.verifyNoInteractions(destinationService, activityRepository);
    }
}