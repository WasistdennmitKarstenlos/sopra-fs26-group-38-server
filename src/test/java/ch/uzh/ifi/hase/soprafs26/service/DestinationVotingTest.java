package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.entity.Destination;
import ch.uzh.ifi.hase.soprafs26.entity.DestinationVote;
import ch.uzh.ifi.hase.soprafs26.entity.Trip;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.entity.VoteType;
import ch.uzh.ifi.hase.soprafs26.repository.DestinationRepository;
import ch.uzh.ifi.hase.soprafs26.repository.DestinationVoteRepository;
import ch.uzh.ifi.hase.soprafs26.rest.dto.DestinationVoteRequestDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.DestinationVoteResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Integration tests for destination voting functionality.
 * Tests cover: successful voting, vote changes (toggle), unauthorized access, unknown destination.
 */
public class DestinationVotingTest {

    @Mock
    private DestinationRepository destinationRepository;

    @Mock
    private DestinationVoteRepository destinationVoteRepository;

    @Mock
    private TripService tripService;

    @InjectMocks
    private DestinationService destinationService;

    private Destination testDestination;
    private User testUser;
    private Trip testTrip;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);

        // Create test data
        testTrip = new Trip("Test Trip", "TEST-ROOM", 1L);
        testTrip.setId(1L);
        testTrip.setStatus(Trip.TripStatus.ACTIVE);

        testUser = new User();
        testUser.setId(123L);
        testUser.setUsername("testuser");

        testDestination = new Destination();
        testDestination.setId(50L);
        testDestination.setTripId(1L);
        testDestination.setDestinationName("Zurich");
        testDestination.setProposedByUserId(1L);
    }

    /**
     * US-13: Test successful upvote on a destination
     */
    @Test
    public void voteOnDestination_upvote_success() {
        DestinationVoteRequestDTO voteRequest = new DestinationVoteRequestDTO();
        voteRequest.setVoteType("UP");

        Mockito.when(destinationRepository.findByIdAndTripId(50L, 1L)).thenReturn(Optional.of(testDestination));
        Mockito.when(tripService.getTripForParticipant(1L, 123L)).thenReturn(testTrip);
        Mockito.when(destinationVoteRepository.findByDestinationIdAndUserId(50L, 123L)).thenReturn(Optional.empty());
        Mockito.when(destinationVoteRepository.countByDestinationIdAndVoteType(50L, VoteType.UP)).thenReturn(1L);
        Mockito.when(destinationVoteRepository.countByDestinationIdAndVoteType(50L, VoteType.DOWN)).thenReturn(0L);

        DestinationVoteResponseDTO response = destinationService.voteOnDestination(1L, 50L, 123L, voteRequest);

        assertEquals(50L, response.getDestinationId());
        assertEquals(1L, response.getUpvotes());
        assertEquals(0L, response.getDownvotes());
        assertEquals(1L, response.getScore());
        assertNull(response.getUserVote());

        Mockito.verify(destinationVoteRepository, Mockito.times(1)).save(Mockito.any(DestinationVote.class));
    }

    /**
     * US-13: Test successful downvote on a destination
     */
    @Test
    public void voteOnDestination_downvote_success() {
        DestinationVoteRequestDTO voteRequest = new DestinationVoteRequestDTO();
        voteRequest.setVoteType("DOWN");

        Mockito.when(destinationRepository.findByIdAndTripId(50L, 1L)).thenReturn(Optional.of(testDestination));
        Mockito.when(tripService.getTripForParticipant(1L, 123L)).thenReturn(testTrip);
        Mockito.when(destinationVoteRepository.findByDestinationIdAndUserId(50L, 123L)).thenReturn(Optional.empty());
        Mockito.when(destinationVoteRepository.countByDestinationIdAndVoteType(50L, VoteType.UP)).thenReturn(0L);
        Mockito.when(destinationVoteRepository.countByDestinationIdAndVoteType(50L, VoteType.DOWN)).thenReturn(1L);

        DestinationVoteResponseDTO response = destinationService.voteOnDestination(1L, 50L, 123L, voteRequest);

        assertEquals(50L, response.getDestinationId());
        assertEquals(0L, response.getUpvotes());
        assertEquals(1L, response.getDownvotes());
        assertEquals(-1L, response.getScore());

        Mockito.verify(destinationVoteRepository, Mockito.times(1)).save(Mockito.any(DestinationVote.class));
    }

    /**
     * US-13: Test vote change - user changes from DOWN to UP
     */
    @Test
    public void voteOnDestination_changeFromDownToUp_success() {
        DestinationVoteRequestDTO voteRequest = new DestinationVoteRequestDTO();
        voteRequest.setVoteType("UP");

        DestinationVote existingDownVote = new DestinationVote(50L, 123L, VoteType.DOWN);

        Mockito.when(destinationRepository.findByIdAndTripId(50L, 1L)).thenReturn(Optional.of(testDestination));
        Mockito.when(tripService.getTripForParticipant(1L, 123L)).thenReturn(testTrip);
        Mockito.when(destinationVoteRepository.findByDestinationIdAndUserId(50L, 123L)).thenReturn(Optional.of(existingDownVote));
        Mockito.when(destinationVoteRepository.countByDestinationIdAndVoteType(50L, VoteType.UP)).thenReturn(1L);
        Mockito.when(destinationVoteRepository.countByDestinationIdAndVoteType(50L, VoteType.DOWN)).thenReturn(0L);

        DestinationVoteResponseDTO response = destinationService.voteOnDestination(1L, 50L, 123L, voteRequest);

        assertEquals(1L, response.getScore());
        assertEquals(1L, response.getUpvotes());
        assertEquals(0L, response.getDownvotes());

        Mockito.verify(destinationVoteRepository, Mockito.times(1)).save(Mockito.any(DestinationVote.class));
    }

    /**
     * US-13: Test toggle vote - user removes existing vote by voting same type again
     */
    @Test
    public void voteOnDestination_toggleRemovesVote_success() {
        DestinationVoteRequestDTO voteRequest = new DestinationVoteRequestDTO();
        voteRequest.setVoteType("DOWN");

        DestinationVote existingDownVote = new DestinationVote(50L, 123L, VoteType.DOWN);

        Mockito.when(destinationRepository.findByIdAndTripId(50L, 1L)).thenReturn(Optional.of(testDestination));
        Mockito.when(tripService.getTripForParticipant(1L, 123L)).thenReturn(testTrip);
        Mockito.when(destinationVoteRepository.findByDestinationIdAndUserId(50L, 123L)).thenReturn(Optional.of(existingDownVote));
        Mockito.when(destinationVoteRepository.countByDestinationIdAndVoteType(50L, VoteType.UP)).thenReturn(0L);
        Mockito.when(destinationVoteRepository.countByDestinationIdAndVoteType(50L, VoteType.DOWN)).thenReturn(0L);

        DestinationVoteResponseDTO response = destinationService.voteOnDestination(1L, 50L, 123L, voteRequest);

        assertEquals(0L, response.getScore());
        assertEquals(0L, response.getUpvotes());
        assertEquals(0L, response.getDownvotes());

        Mockito.verify(destinationVoteRepository, Mockito.times(1)).delete(Mockito.any(DestinationVote.class));
    }

    /**
     * US-13: Test unauthorized access - trip is not active
     */
    @Test
    public void voteOnDestination_tripNotActive_throwsException() {
        DestinationVoteRequestDTO voteRequest = new DestinationVoteRequestDTO();
        voteRequest.setVoteType("UP");

        Trip inactiveTrip = new Trip("Test Trip", "TEST-ROOM", 1L);
        inactiveTrip.setId(1L);
        inactiveTrip.setStatus(Trip.TripStatus.EVALUATION);

        Mockito.when(destinationRepository.findByIdAndTripId(50L, 1L)).thenReturn(Optional.of(testDestination));
        Mockito.when(tripService.getTripForParticipant(1L, 123L)).thenReturn(inactiveTrip);

        assertThrows(ResponseStatusException.class,
                () -> destinationService.voteOnDestination(1L, 50L, 123L, voteRequest));
    }

    /**
     * US-13: Test unauthorized access - user not a trip participant
     */
    @Test
    public void voteOnDestination_userNotParticipant_throwsException() {
        DestinationVoteRequestDTO voteRequest = new DestinationVoteRequestDTO();
        voteRequest.setVoteType("UP");

        Mockito.when(destinationRepository.findByIdAndTripId(50L, 1L)).thenReturn(Optional.of(testDestination));
        Mockito.when(tripService.getTripForParticipant(1L, 123L))
                .thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "Not a participant"));

        assertThrows(ResponseStatusException.class,
                () -> destinationService.voteOnDestination(1L, 50L, 123L, voteRequest));
    }

    /**
     * US-13: Test unknown destination - destination does not exist
     */
    @Test
    public void voteOnDestination_destinationNotFound_throwsException() {
        DestinationVoteRequestDTO voteRequest = new DestinationVoteRequestDTO();
        voteRequest.setVoteType("UP");

        Mockito.when(destinationRepository.findByIdAndTripId(50L, 1L)).thenReturn(Optional.empty());
        Mockito.when(tripService.getTripForParticipant(1L, 123L)).thenReturn(testTrip);

        assertThrows(ResponseStatusException.class,
                () -> destinationService.voteOnDestination(1L, 50L, 123L, voteRequest));
    }

    /**
     * US-13: Test invalid vote type
     */
    @Test
    public void voteOnDestination_invalidVoteType_throwsException() {
        DestinationVoteRequestDTO voteRequest = new DestinationVoteRequestDTO();
        voteRequest.setVoteType("MAYBE");

        Mockito.when(destinationRepository.findByIdAndTripId(50L, 1L)).thenReturn(Optional.of(testDestination));
        Mockito.when(tripService.getTripForParticipant(1L, 123L)).thenReturn(testTrip);

        assertThrows(ResponseStatusException.class,
                () -> destinationService.voteOnDestination(1L, 50L, 123L, voteRequest));
    }

    /**
     * US-13: Test null vote request
     */
    @Test
    public void voteOnDestination_nullVoteRequest_throwsException() {
        Mockito.when(destinationRepository.findByIdAndTripId(50L, 1L)).thenReturn(Optional.of(testDestination));
        Mockito.when(tripService.getTripForParticipant(1L, 123L)).thenReturn(testTrip);

        assertThrows(ResponseStatusException.class,
                () -> destinationService.voteOnDestination(1L, 50L, 123L, null));
    }

    /**
     * US-13: Test score calculation with multiple votes
     */
    @Test
    public void voteOnDestination_scoreCalculation_correct() {
        DestinationVoteRequestDTO voteRequest = new DestinationVoteRequestDTO();
        voteRequest.setVoteType("UP");

        Mockito.when(destinationRepository.findByIdAndTripId(50L, 1L)).thenReturn(Optional.of(testDestination));
        Mockito.when(tripService.getTripForParticipant(1L, 123L)).thenReturn(testTrip);
        Mockito.when(destinationVoteRepository.findByDestinationIdAndUserId(50L, 123L)).thenReturn(Optional.empty());
        Mockito.when(destinationVoteRepository.countByDestinationIdAndVoteType(50L, VoteType.UP)).thenReturn(8L);
        Mockito.when(destinationVoteRepository.countByDestinationIdAndVoteType(50L, VoteType.DOWN)).thenReturn(3L);

        DestinationVoteResponseDTO response = destinationService.voteOnDestination(1L, 50L, 123L, voteRequest);

        assertEquals(8L, response.getUpvotes());
        assertEquals(3L, response.getDownvotes());
        assertEquals(5L, response.getScore()); // 8 - 3 = 5
    }
}
