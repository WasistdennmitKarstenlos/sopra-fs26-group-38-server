package ch.uzh.ifi.hase.soprafs26.service;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.entity.Activity;
import ch.uzh.ifi.hase.soprafs26.entity.Destination;
import ch.uzh.ifi.hase.soprafs26.entity.Vote;
import ch.uzh.ifi.hase.soprafs26.entity.VoteType;
import ch.uzh.ifi.hase.soprafs26.repository.ActivityRepository;
import ch.uzh.ifi.hase.soprafs26.repository.DestinationRepository;
import ch.uzh.ifi.hase.soprafs26.repository.VoteRepository;
import ch.uzh.ifi.hase.soprafs26.rest.dto.DestinationGetDTO;

public class DestinationServiceTest {

    @Mock
    private DestinationRepository destinationRepository;

    @Mock
    private ActivityRepository activityRepository;

    @Mock
    private VoteRepository voteRepository;

    @Mock
    private TripService tripService;

    @InjectMocks
    private DestinationService destinationService;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void createDestination_readOnlyMode_badRequest() {
        Destination destination = new Destination();
        destination.setDestinationName("Zurich");

        Mockito.doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "read-only"))
                .when(tripService).ensureTripIsActiveForMutations(1L);

        assertThrows(ResponseStatusException.class, () -> destinationService.createDestination(1L, destination));
        Mockito.verifyNoInteractions(destinationRepository);
    }

    @Test
    public void createDestination_duplicateName_conflict() {
        Destination destination = new Destination();
        destination.setDestinationName(" zurich ");

        Mockito.when(destinationRepository.existsByTripIdAndDestinationNameIgnoreCase(1L, "zurich"))
                .thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> destinationService.createDestination(1L, destination));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        Mockito.verify(destinationRepository, Mockito.never()).save(Mockito.any());
    }

    @Test
    public void updateDestination_readOnlyMode_badRequest() {
        Destination update = new Destination();
        update.setDestinationName("Basel");

        Mockito.doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "read-only"))
                .when(tripService).ensureTripIsActiveForMutations(1L);

        assertThrows(ResponseStatusException.class, () -> destinationService.updateDestination(1L, 11L, update, 1L));
        Mockito.verifyNoInteractions(destinationRepository);
    }

    @Test
    public void deleteDestination_readOnlyMode_badRequest() {
        Mockito.doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "read-only"))
                .when(tripService).ensureTripIsActiveForMutations(1L);

        assertThrows(ResponseStatusException.class, () -> destinationService.deleteDestination(1L, 11L, 1L));
        Mockito.verifyNoInteractions(destinationRepository);
    }

    @Test
    public void updateDestination_activeMode_success() {
        Destination existing = new Destination();
        existing.setId(11L);
        existing.setTripId(1L);
        existing.setProposedByUserId(1L);
        existing.setDestinationName("Old Name");

        Destination update = new Destination();
        update.setDestinationName("New Name");

        Mockito.when(destinationRepository.findByIdAndTripId(11L, 1L)).thenReturn(Optional.of(existing));
        Mockito.when(destinationRepository.save(Mockito.any(Destination.class))).thenReturn(existing);

        destinationService.updateDestination(1L, 11L, update, 1L);

        Mockito.verify(destinationRepository, Mockito.times(1)).save(existing);
    }

    @Test
    public void updateDestination_duplicateName_conflict() {
        Destination existing = new Destination();
        existing.setId(11L);
        existing.setTripId(1L);
        existing.setDestinationName("Old Name");

        Destination update = new Destination();
        update.setDestinationName("Zurich");

        Mockito.when(destinationRepository.findByIdAndTripId(11L, 1L)).thenReturn(Optional.of(existing));
        Mockito.when(destinationRepository.existsByTripIdAndDestinationNameIgnoreCaseAndIdNot(1L, "Zurich", 11L))
                .thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> destinationService.updateDestination(1L, 11L, update, 1L));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        Mockito.verify(destinationRepository, Mockito.never()).save(Mockito.any());
    }

    @Test
    public void populateDestinationVoteData_noActivities_setsZeroValues() {
        Destination destination = new Destination();
        destination.setId(11L);
        destination.setTripId(1L);

        DestinationGetDTO dto = new DestinationGetDTO();

        Mockito.when(activityRepository.findByTripIdAndDestinationIdOrderByIdDesc(1L, 11L))
                .thenReturn(List.of());

        destinationService.populateDestinationVoteData(destination, 123L, dto);

        assertEquals(0L, dto.getUpvotes());
        assertEquals(0L, dto.getDownvotes());
        assertEquals(0.0, dto.getScore());
        assertNull(dto.getUserVote());
    }

    @Test
        public void populateDestinationVoteData_withActivities_usesWilsonRanking() {
        Destination destination = new Destination();
        destination.setId(11L);
        destination.setTripId(1L);

        Activity a1 = new Activity();
        a1.setId(100L);
        Activity a2 = new Activity();
        a2.setId(101L);
        Activity a3 = new Activity();
        a3.setId(102L);

        DestinationGetDTO dto = new DestinationGetDTO();

        Mockito.when(activityRepository.findByTripIdAndDestinationIdOrderByIdDesc(1L, 11L))
                .thenReturn(List.of(a1, a2, a3));

        // Wilson-based hybrid score is conservative and rounds to 0 for this small sample.
        Mockito.when(voteRepository.findByActivityIdIn(List.of(100L, 101L, 102L)))
                .thenReturn(List.of(
                        new Vote(100L, 1L, VoteType.UP),
                        new Vote(100L, 2L, VoteType.UP),
                        new Vote(101L, 3L, VoteType.DOWN),
                        new Vote(102L, 4L, VoteType.UP)
                ));

        destinationService.populateDestinationVoteData(destination, 123L, dto);

        assertEquals(3L, dto.getUpvotes());
        assertEquals(1L, dto.getDownvotes());
        assertNotNull(dto.getScore());
        assertNull(dto.getUserVote());
    }

    @Test
    public void deleteDestination_success() {
        Destination existing = new Destination();
        existing.setId(11L);
        existing.setTripId(1L);
        existing.setProposedByUserId(1L);
        existing.setDestinationName("Paris");

        Mockito.when(destinationRepository.findByIdAndTripId(11L, 1L)).thenReturn(Optional.of(existing));
        Mockito.when(activityRepository.findByTripIdAndDestinationIdOrderByIdDesc(1L, 11L))
                .thenReturn(List.of());

        destinationService.deleteDestination(1L, 11L, 1L);

        Mockito.verify(destinationRepository, Mockito.times(1)).delete(existing);
    }

    @Test
    public void deleteDestination_forbidden_notCreator() {
        Destination existing = new Destination();
        existing.setId(11L);
        existing.setTripId(1L);
        existing.setProposedByUserId(1L);
        existing.setDestinationName("Paris");

        Mockito.when(destinationRepository.findByIdAndTripId(11L, 1L)).thenReturn(Optional.of(existing));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> destinationService.deleteDestination(1L, 11L, 2L));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        Mockito.verify(destinationRepository, Mockito.never()).delete(Mockito.any());
    }

    @Test
    public void deleteDestination_conflict_hasActivities() {
        Destination existing = new Destination();
        existing.setId(11L);
        existing.setTripId(1L);
        existing.setProposedByUserId(1L);
        existing.setDestinationName("Paris");

        Activity activity = new Activity();
        activity.setId(100L);

        Mockito.when(destinationRepository.findByIdAndTripId(11L, 1L)).thenReturn(Optional.of(existing));
        Mockito.when(activityRepository.findByTripIdAndDestinationIdOrderByIdDesc(1L, 11L))
                .thenReturn(List.of(activity));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> destinationService.deleteDestination(1L, 11L, 1L));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        Mockito.verify(destinationRepository, Mockito.never()).delete(Mockito.any());
    }
}
