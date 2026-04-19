package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.entity.Activity;
import ch.uzh.ifi.hase.soprafs26.entity.Trip;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.entity.Vote;
import ch.uzh.ifi.hase.soprafs26.entity.VoteType;
import ch.uzh.ifi.hase.soprafs26.entity.Destination;
import ch.uzh.ifi.hase.soprafs26.repository.ActivityRepository;
import ch.uzh.ifi.hase.soprafs26.repository.VoteRepository;
import ch.uzh.ifi.hase.soprafs26.rest.dto.ActivityVoteRequestDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.ActivityVoteResponseDTO;
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

import org.springframework.context.ApplicationEventPublisher;

/**
 * Integration tests for activity voting functionality.
 * Tests cover: successful voting, vote changes (toggle), unauthorized access, unknown activity.
 */
public class ActivityVotingTest {

    @Mock
    private ActivityRepository activityRepository;

    @Mock
    private VoteRepository voteRepository;

    @Mock
    private DestinationService destinationService;

    @Mock
    private TripService tripService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ActivityManagementService activityManagementService;

    private Activity testActivity;
        private User testUser;
        private Destination testDestination;
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

        testActivity = new Activity();
        testActivity.setId(100L);
        testActivity.setTripId(1L);
        testActivity.setDestinationId(2L);
        testActivity.setPlaceId("place-museum");
        testActivity.setName("City Museum");
            testDestination = new Destination();
            testDestination.setId(2L);
            testDestination.setDestinationName("Test Destination");
    }

    /**
     * US-13: Test successful upvote on an activity
     */
    @Test
    public void voteOnActivity_upvote_success() {
        ActivityVoteRequestDTO voteRequest = new ActivityVoteRequestDTO();
        voteRequest.setVoteType("UP");

        Mockito.when(activityRepository.findById(100L)).thenReturn(Optional.of(testActivity));
        Mockito.when(activityRepository.findByIdAndTripIdAndDestinationId(100L, 1L, 2L)).thenReturn(Optional.of(testActivity));
        Mockito.when(tripService.getTripForParticipant(1L, 123L)).thenReturn(testTrip);
            Mockito.when(destinationService.getDestinationEntity(1L, 2L)).thenReturn(testDestination);
        Mockito.when(voteRepository.findByActivityIdAndUserId(100L, 123L)).thenReturn(Optional.empty());
        Mockito.when(voteRepository.countByActivityIdAndVoteType(100L, VoteType.UP)).thenReturn(1L);
        Mockito.when(voteRepository.countByActivityIdAndVoteType(100L, VoteType.DOWN)).thenReturn(0L);

        ActivityVoteResponseDTO response = activityManagementService.voteOnActivity(100L, 123L, voteRequest);

        assertEquals(100L, response.getActivityId());
        assertEquals(1L, response.getUpvotes());
        assertEquals(0L, response.getDownvotes());
        assertEquals(1L, response.getScore());
        assertNull(response.getUserVote()); // Previous vote state before the save

        Mockito.verify(voteRepository, Mockito.times(1)).save(Mockito.any(Vote.class));
    }

    /**
     * US-13: Test successful downvote on an activity
     */
    @Test
    public void voteOnActivity_downvote_success() {
        ActivityVoteRequestDTO voteRequest = new ActivityVoteRequestDTO();
        voteRequest.setVoteType("DOWN");

        Mockito.when(activityRepository.findById(100L)).thenReturn(Optional.of(testActivity));
        Mockito.when(activityRepository.findByIdAndTripIdAndDestinationId(100L, 1L, 2L)).thenReturn(Optional.of(testActivity));
        Mockito.when(tripService.getTripForParticipant(1L, 123L)).thenReturn(testTrip);
            Mockito.when(destinationService.getDestinationEntity(1L, 2L)).thenReturn(testDestination);
        Mockito.when(voteRepository.findByActivityIdAndUserId(100L, 123L)).thenReturn(Optional.empty());
        Mockito.when(voteRepository.countByActivityIdAndVoteType(100L, VoteType.UP)).thenReturn(0L);
        Mockito.when(voteRepository.countByActivityIdAndVoteType(100L, VoteType.DOWN)).thenReturn(1L);

        ActivityVoteResponseDTO response = activityManagementService.voteOnActivity(100L, 123L, voteRequest);

        assertEquals(100L, response.getActivityId());
        assertEquals(0L, response.getUpvotes());
        assertEquals(1L, response.getDownvotes());
        assertEquals(-1L, response.getScore());

        Mockito.verify(voteRepository, Mockito.times(1)).save(Mockito.any(Vote.class));
    }

    /**
     * US-13: Test vote change - user changes from UP to DOWN
     */
    @Test
    public void voteOnActivity_changeFromUptoDown_success() {
        ActivityVoteRequestDTO voteRequest = new ActivityVoteRequestDTO();
        voteRequest.setVoteType("DOWN");

        Vote existingUpVote = new Vote(100L, 123L, VoteType.UP);

        Mockito.when(activityRepository.findById(100L)).thenReturn(Optional.of(testActivity));
        Mockito.when(activityRepository.findByIdAndTripIdAndDestinationId(100L, 1L, 2L)).thenReturn(Optional.of(testActivity));
        Mockito.when(tripService.getTripForParticipant(1L, 123L)).thenReturn(testTrip);
            Mockito.when(destinationService.getDestinationEntity(1L, 2L)).thenReturn(testDestination);
        Mockito.when(voteRepository.findByActivityIdAndUserId(100L, 123L)).thenReturn(Optional.of(existingUpVote));
        Mockito.when(voteRepository.countByActivityIdAndVoteType(100L, VoteType.UP)).thenReturn(0L);
        Mockito.when(voteRepository.countByActivityIdAndVoteType(100L, VoteType.DOWN)).thenReturn(1L);

        ActivityVoteResponseDTO response = activityManagementService.voteOnActivity(100L, 123L, voteRequest);

        assertEquals(-1L, response.getScore());
        assertEquals(0L, response.getUpvotes());
        assertEquals(1L, response.getDownvotes());

        Mockito.verify(voteRepository, Mockito.times(1)).save(Mockito.any(Vote.class));
    }

    /**
     * US-13: Test toggle vote - user removes existing vote by voting same type again
     */
    @Test
    public void voteOnActivity_toggleRemovesVote_success() {
        ActivityVoteRequestDTO voteRequest = new ActivityVoteRequestDTO();
        voteRequest.setVoteType("UP");

        Vote existingUpVote = new Vote(100L, 123L, VoteType.UP);

        Mockito.when(activityRepository.findById(100L)).thenReturn(Optional.of(testActivity));
        Mockito.when(activityRepository.findByIdAndTripIdAndDestinationId(100L, 1L, 2L)).thenReturn(Optional.of(testActivity));
        Mockito.when(tripService.getTripForParticipant(1L, 123L)).thenReturn(testTrip);
            Mockito.when(destinationService.getDestinationEntity(1L, 2L)).thenReturn(testDestination);
        Mockito.when(voteRepository.findByActivityIdAndUserId(100L, 123L)).thenReturn(Optional.of(existingUpVote));
        Mockito.when(voteRepository.countByActivityIdAndVoteType(100L, VoteType.UP)).thenReturn(0L);
        Mockito.when(voteRepository.countByActivityIdAndVoteType(100L, VoteType.DOWN)).thenReturn(0L);

        ActivityVoteResponseDTO response = activityManagementService.voteOnActivity(100L, 123L, voteRequest);

        assertEquals(0L, response.getScore());
        assertEquals(0L, response.getUpvotes());
        assertEquals(0L, response.getDownvotes());

        Mockito.verify(voteRepository, Mockito.times(1)).delete(Mockito.any(Vote.class));
    }

    /**
     * US-13: Test unauthorized access - trip is not active
     */
    @Test
    public void voteOnActivity_tripNotActive_throwsException() {
        ActivityVoteRequestDTO voteRequest = new ActivityVoteRequestDTO();
        voteRequest.setVoteType("UP");

        Trip inactiveTrip = new Trip("Test Trip", "TEST-ROOM", 1L);
        inactiveTrip.setId(1L);
        inactiveTrip.setStatus(Trip.TripStatus.FINALIZED);

        Mockito.when(activityRepository.findById(100L)).thenReturn(Optional.of(testActivity));
        Mockito.when(tripService.getTripForParticipant(1L, 123L)).thenReturn(inactiveTrip);

        assertThrows(ResponseStatusException.class,
                () -> activityManagementService.voteOnActivity(100L, 123L, voteRequest));
    }

    /**
     * US-13: Test unauthorized access - user not a trip participant
     */
    @Test
    public void voteOnActivity_userNotParticipant_throwsException() {
        ActivityVoteRequestDTO voteRequest = new ActivityVoteRequestDTO();
        voteRequest.setVoteType("UP");

        Mockito.when(activityRepository.findById(100L)).thenReturn(Optional.of(testActivity));
        Mockito.when(tripService.getTripForParticipant(1L, 123L))
                .thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "Not a participant"));

        assertThrows(ResponseStatusException.class,
                () -> activityManagementService.voteOnActivity(100L, 123L, voteRequest));
    }

    /**
     * US-13: Test unknown activity - activity does not exist
     */
    @Test
    public void voteOnActivity_activityNotFound_throwsException() {
        ActivityVoteRequestDTO voteRequest = new ActivityVoteRequestDTO();
        voteRequest.setVoteType("UP");

        Mockito.when(activityRepository.findById(100L)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class,
                () -> activityManagementService.voteOnActivity(100L, 123L, voteRequest));
    }

    /**
     * US-13: Test invalid vote type
     */
    @Test
    public void voteOnActivity_invalidVoteType_throwsException() {
        ActivityVoteRequestDTO voteRequest = new ActivityVoteRequestDTO();
        voteRequest.setVoteType("INVALID");

        Mockito.when(activityRepository.findById(100L)).thenReturn(Optional.of(testActivity));
        Mockito.when(tripService.getTripForParticipant(1L, 123L)).thenReturn(testTrip);

        assertThrows(ResponseStatusException.class,
                () -> activityManagementService.voteOnActivity(100L, 123L, voteRequest));
    }

    /**
     * US-13: Test null vote request
     */
    @Test
    public void voteOnActivity_nullVoteRequest_throwsException() {
        Mockito.when(activityRepository.findById(100L)).thenReturn(Optional.of(testActivity));
        Mockito.when(activityRepository.findByIdAndTripIdAndDestinationId(100L, 1L, 2L)).thenReturn(Optional.of(testActivity));
        Mockito.when(tripService.getTripForParticipant(1L, 123L)).thenReturn(testTrip);
            Mockito.when(destinationService.getDestinationEntity(1L, 2L)).thenReturn(testDestination);

        assertThrows(ResponseStatusException.class,
                () -> activityManagementService.voteOnActivity(100L, 123L, null));
    }

    /**
     * US-13: Test score calculation with multiple votes
     */
    @Test
    public void voteOnActivity_scoreCalculation_correct() {
        ActivityVoteRequestDTO voteRequest = new ActivityVoteRequestDTO();
        voteRequest.setVoteType("UP");

        Mockito.when(activityRepository.findById(100L)).thenReturn(Optional.of(testActivity));
        Mockito.when(activityRepository.findByIdAndTripIdAndDestinationId(100L, 1L, 2L)).thenReturn(Optional.of(testActivity));
        Mockito.when(tripService.getTripForParticipant(1L, 123L)).thenReturn(testTrip);
            Mockito.when(destinationService.getDestinationEntity(1L, 2L)).thenReturn(testDestination);
        Mockito.when(voteRepository.findByActivityIdAndUserId(100L, 123L)).thenReturn(Optional.empty());
        Mockito.when(voteRepository.countByActivityIdAndVoteType(100L, VoteType.UP)).thenReturn(5L);
        Mockito.when(voteRepository.countByActivityIdAndVoteType(100L, VoteType.DOWN)).thenReturn(2L);

        ActivityVoteResponseDTO response = activityManagementService.voteOnActivity(100L, 123L, voteRequest);

        assertEquals(5L, response.getUpvotes());
        assertEquals(2L, response.getDownvotes());
        assertEquals(3L, response.getScore()); // 5 - 2 = 3
    }
}
