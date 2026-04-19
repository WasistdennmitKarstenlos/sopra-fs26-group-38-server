package ch.uzh.ifi.hase.soprafs26.service;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.entity.Destination;
import ch.uzh.ifi.hase.soprafs26.entity.Trip;
import ch.uzh.ifi.hase.soprafs26.entity.TripMembership;
import ch.uzh.ifi.hase.soprafs26.repository.DestinationRepository;
import ch.uzh.ifi.hase.soprafs26.repository.TripMembershipRepository;
import ch.uzh.ifi.hase.soprafs26.repository.TripRepository;

/**
 * TripServiceTest
 * Unit tests for TripService business logic
 */
public class TripServiceTest {

    @Mock
    private TripRepository tripRepository;

    @Mock
    private DestinationRepository destinationRepository;

    @Mock
    private TripMembershipRepository tripMembershipRepository;


    @InjectMocks
    private TripService tripService;

    private Trip testTrip;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);

        // given
        testTrip = new Trip();
        testTrip.setId(1L);
        testTrip.setName("Paris Vacation");
        testTrip.setRoomCode("ABC123");
        testTrip.setHostId(1L);
        testTrip.setStatus(Trip.TripStatus.ACTIVE);

        // when -> any object is being saved in the tripRepository -> return the dummy testTrip
        Mockito.when(tripRepository.save(Mockito.any())).thenReturn(testTrip);
        Mockito.when(tripMembershipRepository.existsByTripIdAndUserId(Mockito.anyLong(), Mockito.anyLong()))
            .thenReturn(true);
    }

    @Test
    public void createTrip_validInputs_success() {
        // given
        Trip tripInput = new Trip();
        tripInput.setName("Paris Vacation");
        tripInput.setHostId(1L);

        // when
        Trip createdTrip = tripService.createTrip(tripInput);

        // then
        Mockito.verify(tripRepository, Mockito.times(1)).save(Mockito.any());
        assertNotNull(createdTrip.getRoomCode());
        assertEquals("Paris Vacation", createdTrip.getName());
        assertEquals(1L, createdTrip.getHostId());
        assertEquals(Trip.TripStatus.ACTIVE, createdTrip.getStatus());
    }

    @Test
    public void createTrip_invalidName_throwsException() {
        // given
        Trip tripInput = new Trip();
        tripInput.setName("");
        tripInput.setHostId(1L);

        // when/then
        assertThrows(ResponseStatusException.class, () -> tripService.createTrip(tripInput));
    }

    @Test
    public void createTrip_nullName_throwsException() {
        // given
        Trip tripInput = new Trip();
        tripInput.setName(null);
        tripInput.setHostId(1L);

        // when/then
        assertThrows(ResponseStatusException.class, () -> tripService.createTrip(tripInput));
    }

    @Test
    public void createTrip_nullHostId_throwsException() {
        // given
        Trip tripInput = new Trip();
        tripInput.setName("Paris Vacation");
        tripInput.setHostId(null);

        // when/then
        assertThrows(ResponseStatusException.class, () -> tripService.createTrip(tripInput));
    }

    @Test
    public void createTrip_duplicateName_throwsException() {
        // given
        Trip tripInput = new Trip();
        tripInput.setName("Paris Vacation");
        tripInput.setHostId(1L);

        // when -> setup additional mocks
        Mockito.when(tripRepository.findByNameAndHostId("Paris Vacation", 1L))
                .thenReturn(Optional.of(testTrip));

        // then -> attempt to create duplicate trip -> check that an error is thrown
        assertThrows(ResponseStatusException.class, () -> tripService.createTrip(tripInput));
    }

    @Test
    public void getTripById_validId_success() {
        // when
        Mockito.when(tripRepository.findById(1L)).thenReturn(Optional.of(testTrip));

        Trip retrievedTrip = tripService.getTripById(1L);

        // then
        assertEquals(testTrip.getId(), retrievedTrip.getId());
        assertEquals(testTrip.getName(), retrievedTrip.getName());
        assertEquals(testTrip.getRoomCode(), retrievedTrip.getRoomCode());
    }

    @Test
    public void getTripById_invalidId_throwsException() {
        // when
        Mockito.when(tripRepository.findById(999L)).thenReturn(Optional.empty());

        // then
        assertThrows(ResponseStatusException.class, () -> tripService.getTripById(999L));
    }

    @Test
    public void getTripByRoomCode_validCode_success() {
        // when
        Mockito.when(tripRepository.findByRoomCode("ABC123")).thenReturn(Optional.of(testTrip));

        Trip retrievedTrip = tripService.getTripByRoomCode("ABC123");

        // then
        assertEquals("ABC123", retrievedTrip.getRoomCode());
        assertEquals("Paris Vacation", retrievedTrip.getName());
    }

    @Test
    public void getTripByRoomCode_invalidCode_throwsException() {
        // when
        Mockito.when(tripRepository.findByRoomCode("INVALID")).thenReturn(Optional.empty());

        // then
        assertThrows(ResponseStatusException.class, () -> tripService.getTripByRoomCode("INVALID"));
    }

    @Test
    public void getTripsByHostId_validHostId_success() {
        // given
        Trip trip2 = new Trip();
        trip2.setId(2L);
        trip2.setName("London Trip");
        trip2.setHostId(1L);
        
        List<Trip> trips = Arrays.asList(testTrip, trip2);

        // when
        Mockito.when(tripRepository.findByHostId(1L)).thenReturn(trips);

        List<Trip> retrievedTrips = tripService.getTripsByHostId(1L);

        // then
        assertEquals(2, retrievedTrips.size());
        assertEquals("Paris Vacation", retrievedTrips.get(0).getName());
        assertEquals("London Trip", retrievedTrips.get(1).getName());
    }

    @Test
    public void updateTripStatus_validInput_success() {
        // given
        Mockito.when(tripRepository.findById(1L)).thenReturn(Optional.of(testTrip));
        Destination existingDestination = new Destination();
        existingDestination.setId(10L);
        existingDestination.setTripId(1L);
        existingDestination.setDestinationName("Zurich");
        Mockito.when(destinationRepository.findByTripIdOrderByIdDesc(1L)).thenReturn(List.of(existingDestination));

        // when
        Trip updatedTrip = tripService.updateTripStatus(1L, Trip.TripStatus.EVALUATION);

        // then
        Mockito.verify(tripRepository, Mockito.times(1)).save(Mockito.any());
        assertEquals(Trip.TripStatus.EVALUATION, updatedTrip.getStatus());
    }

    @Test
    public void updateTripStatus_noDestinations_badRequest() {
        Mockito.when(tripRepository.findById(1L)).thenReturn(Optional.of(testTrip));
        Mockito.when(destinationRepository.findByTripIdOrderByIdDesc(1L)).thenReturn(List.of());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> tripService.updateTripStatus(1L, Trip.TripStatus.EVALUATION)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    public void updateTripStatus_alreadyEvaluation_conflict() {
        testTrip.setStatus(Trip.TripStatus.EVALUATION);
        Mockito.when(tripRepository.findById(1L)).thenReturn(Optional.of(testTrip));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> tripService.updateTripStatus(1L, Trip.TripStatus.EVALUATION)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    }

    @Test
    public void updateTripStatus_finalized_conflict() {
        testTrip.setStatus(Trip.TripStatus.FINALIZED);
        Mockito.when(tripRepository.findById(1L)).thenReturn(Optional.of(testTrip));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> tripService.updateTripStatus(1L, Trip.TripStatus.EVALUATION)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    }

    @Test
    public void setFinalDestination_validInput_success() {
        // given
        Mockito.when(tripRepository.findById(1L)).thenReturn(Optional.of(testTrip));

        // when
        Trip updatedTrip = tripService.setFinalDestination(1L, 5L);

        // then
        Mockito.verify(tripRepository, Mockito.times(1)).save(Mockito.any());
        assertEquals(5L, updatedTrip.getFinalDestinationId());
        assertEquals(Trip.TripStatus.FINALIZED, updatedTrip.getStatus());
    }

    @Test
    public void getAllTrips_success() {
        // given
        Trip trip2 = new Trip();
        trip2.setId(2L);
        trip2.setName("London Trip");
        
        List<Trip> trips = Arrays.asList(testTrip, trip2);
        Mockito.when(tripRepository.findAll()).thenReturn(trips);

        // when
        List<Trip> retrievedTrips = tripService.getAllTrips();

        // then
        assertEquals(2, retrievedTrips.size());
        Mockito.verify(tripRepository, Mockito.times(1)).findAll();
    }

    @Test
    public void addDestination_validInput_successAndBroadcast() {
        Mockito.when(tripRepository.findById(1L)).thenReturn(Optional.of(testTrip));

        Destination destinationInput = new Destination();
        destinationInput.setDestinationName("Zurich");

        Destination saved = new Destination();
        saved.setId(10L);
        saved.setTripId(1L);
        saved.setDestinationName("Zurich");
        saved.setProposedByUserId(1L);

        Mockito.when(destinationRepository.save(Mockito.any())).thenReturn(saved);

        Destination created = tripService.addDestination(1L, 1L, destinationInput);

        assertEquals(10L, created.getId());
        assertEquals("Zurich", created.getDestinationName());
        Mockito.verify(destinationRepository, Mockito.times(1)).save(Mockito.any());
    }

    @Test
    public void addDestination_nonParticipant_forbidden() {
        Mockito.when(tripRepository.findById(1L)).thenReturn(Optional.of(testTrip));
        Mockito.when(tripMembershipRepository.existsByTripIdAndUserId(1L, 99L)).thenReturn(false);

        Destination destinationInput = new Destination();
        destinationInput.setDestinationName("Bern");

        assertThrows(ResponseStatusException.class, () -> tripService.addDestination(1L, 99L, destinationInput));
    }

    @Test
    public void addDestination_emptyLocation_badRequest() {
        Mockito.when(tripRepository.findById(1L)).thenReturn(Optional.of(testTrip));

        Destination destinationInput = new Destination();
        destinationInput.setDestinationName("   ");

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> tripService.addDestination(1L, 1L, destinationInput));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    public void addDestination_inactiveTrip_badRequest() {
        testTrip.setStatus(Trip.TripStatus.FINALIZED);
        Mockito.when(tripRepository.findById(1L)).thenReturn(Optional.of(testTrip));

        Destination destinationInput = new Destination();
        destinationInput.setDestinationName("Basel");

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> tripService.addDestination(1L, 1L, destinationInput));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    public void getTripsForUser_returnsMemberTrips() {
        // given
        TripMembership membership = new TripMembership(1L, 42L);
        Mockito.when(tripMembershipRepository.findByUserId(42L))
                .thenReturn(List.of(membership));
        Mockito.when(tripRepository.findByIdInOrderByCreationDateDesc(List.of(1L)))
                .thenReturn(List.of(testTrip));

        // when
        List<Trip> result = tripService.getTripsForUser(42L);

        // then
        assertEquals(1, result.size());
        assertEquals(testTrip.getId(), result.get(0).getId());
        Mockito.verify(tripMembershipRepository).findByUserId(42L);
        Mockito.verify(tripRepository).findByIdInOrderByCreationDateDesc(List.of(1L));
    }

    @Test
    public void getTripsForUser_noMemberships_returnsEmpty() {
        // given
        Mockito.when(tripMembershipRepository.findByUserId(99L))
                .thenReturn(List.of());
        Mockito.when(tripRepository.findByIdInOrderByCreationDateDesc(List.of()))
                .thenReturn(List.of());

        // when
        List<Trip> result = tripService.getTripsForUser(99L);

        // then
        assertEquals(0, result.size());
    }

    @Test
    public void joinTripByRoomCode_validInput_success() {
        Mockito.when(tripRepository.findByRoomCode("ABC123")).thenReturn(Optional.of(testTrip));
        Mockito.when(tripMembershipRepository.existsByTripIdAndUserId(1L, 2L)).thenReturn(false);
        Mockito.when(tripMembershipRepository.existsByTripIdAndRoomUsername(1L, "alice")).thenReturn(false);

        Trip joinedTrip = tripService.joinTripByRoomCode("ABC123", 2L, "  alice  ");

        assertEquals(1L, joinedTrip.getId());
        ArgumentCaptor<TripMembership> membershipCaptor = ArgumentCaptor.forClass(TripMembership.class);
        Mockito.verify(tripMembershipRepository).save(membershipCaptor.capture());
        assertEquals(1L, membershipCaptor.getValue().getTripId());
        assertEquals(2L, membershipCaptor.getValue().getUserId());
        assertEquals("alice", membershipCaptor.getValue().getRoomUsername());
    }

    @Test
    public void joinTripByRoomCode_blankRoomCode_badRequest() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> tripService.joinTripByRoomCode("   ", 2L, "alice"));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    public void joinTripByRoomCode_blankRoomUsername_badRequest() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> tripService.joinTripByRoomCode("ABC123", 2L, "   "));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    public void joinTripByRoomCode_duplicateMembership_conflict() {
        Mockito.when(tripRepository.findByRoomCode("ABC123")).thenReturn(Optional.of(testTrip));
        Mockito.when(tripMembershipRepository.existsByTripIdAndUserId(1L, 2L)).thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> tripService.joinTripByRoomCode("ABC123", 2L, "alice"));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertTrue(exception.getReason().contains("already a member"));
    }

    @Test
    public void joinTripByRoomCode_duplicateRoomUsername_conflict() {
        Mockito.when(tripRepository.findByRoomCode("ABC123")).thenReturn(Optional.of(testTrip));
        Mockito.when(tripMembershipRepository.existsByTripIdAndUserId(1L, 2L)).thenReturn(false);
        Mockito.when(tripMembershipRepository.existsByTripIdAndRoomUsername(1L, "alice")).thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> tripService.joinTripByRoomCode("ABC123", 2L, "alice"));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertTrue(exception.getReason().contains("already taken"));
    }

    @Test
    public void joinTripByRoomCode_closedTrip_badRequest() {
        testTrip.setStatus(Trip.TripStatus.FINALIZED);
        Mockito.when(tripRepository.findByRoomCode("ABC123")).thenReturn(Optional.of(testTrip));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> tripService.joinTripByRoomCode("ABC123", 2L, "alice"));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertTrue(exception.getReason().contains("closed"));
    }
}
