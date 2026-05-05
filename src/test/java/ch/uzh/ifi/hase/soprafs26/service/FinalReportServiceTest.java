package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.entity.Activity;
import ch.uzh.ifi.hase.soprafs26.entity.Destination;
import ch.uzh.ifi.hase.soprafs26.entity.Trip;
import ch.uzh.ifi.hase.soprafs26.entity.Vote;
import ch.uzh.ifi.hase.soprafs26.entity.VoteType;
import ch.uzh.ifi.hase.soprafs26.repository.ActivityRepository;
import ch.uzh.ifi.hase.soprafs26.repository.VoteRepository;
import ch.uzh.ifi.hase.soprafs26.rest.dto.FinalReportGetDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FinalReportServiceTest {

    @Mock
    private TripService tripService;

    @Mock
    private DestinationService destinationService;

    @Mock
    private ActivityRepository activityRepository;

    @Mock
    private VoteRepository voteRepository;

    @InjectMocks
    private FinalReportService finalReportService;

    private Trip finalizedTrip;
    private Destination destination;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);

        finalizedTrip = new Trip();
        finalizedTrip.setId(1L);
        finalizedTrip.setName("Paris Vacation");
        finalizedTrip.setRoomCode("ABC123");
        finalizedTrip.setStatus(Trip.TripStatus.FINALIZED);
        finalizedTrip.setFinalDestinationId(10L);

        destination = new Destination();
        destination.setId(10L);
        destination.setTripId(1L);
        destination.setDestinationName("Paris");
        destination.setProposedByUserId(1L);
    }

    @Test
    public void getFinalReport_success_returnsSortedRankedReport() {
        Activity eiffel = new Activity();
        eiffel.setId(100L);
        eiffel.setTripId(1L);
        eiffel.setDestinationId(10L);
        eiffel.setPlaceId("p-eiffel");
        eiffel.setName("Eiffel Tower");

        Activity louvre = new Activity();
        louvre.setId(101L);
        louvre.setTripId(1L);
        louvre.setDestinationId(10L);
        louvre.setPlaceId("p-louvre");
        louvre.setName("Louvre");

        Mockito.when(tripService.getTripForParticipant(1L, 1L)).thenReturn(finalizedTrip);
        Mockito.when(destinationService.getDestinationEntity(1L, 10L)).thenReturn(destination);
        Mockito.when(activityRepository.findByTripIdAndDestinationIdOrderByIdDesc(1L, 10L)).thenReturn(List.of(eiffel, louvre));
        Mockito.when(voteRepository.findByActivityIdIn(List.of(100L, 101L))).thenReturn(List.of(
                new Vote(100L, 1L, VoteType.UP),
                new Vote(100L, 2L, VoteType.UP),
                new Vote(100L, 3L, VoteType.DOWN),
                new Vote(101L, 1L, VoteType.UP)
        ));

        FinalReportGetDTO report = finalReportService.getFinalReport(1L, 1L);

        assertEquals(1L, report.getTripId());
        assertEquals("Paris", report.getWinningDestination().getName());
        assertEquals(2, report.getWinningDestination().getActivities().size());
        assertEquals("Eiffel Tower", report.getWinningDestination().getActivities().get(0).getName());
        assertEquals(1, report.getWinningDestination().getActivities().get(0).getRank());
        assertEquals(2, report.getWinningDestination().getActivities().get(0).getUpvotes());
        assertEquals(1, report.getWinningDestination().getActivities().get(0).getDownvotes());
        assertEquals(1, report.getWinningDestination().getActivities().get(0).getScore());
        assertEquals(2, report.getWinningDestination().getTotalScore());
    }

    @Test
    public void getFinalReport_notFinalized_conflict() {
        Trip activeTrip = new Trip();
        activeTrip.setId(1L);
        activeTrip.setStatus(Trip.TripStatus.ACTIVE);

        Mockito.when(tripService.getTripForParticipant(1L, 1L)).thenReturn(activeTrip);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> finalReportService.getFinalReport(1L, 1L)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertTrue(exception.getReason().contains("finalized"));
    }

    @Test
    public void getFinalReport_nonParticipant_forbidden() {
        Mockito.when(tripService.getTripForParticipant(1L, 99L))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "User is not a participant of this trip"));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> finalReportService.getFinalReport(1L, 99L)
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }
}