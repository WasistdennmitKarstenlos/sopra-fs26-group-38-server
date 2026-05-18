package ch.uzh.ifi.hase.soprafs26.controller;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import org.junit.jupiter.api.Test;
import static org.mockito.BDDMockito.given;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs26.entity.Trip;
import ch.uzh.ifi.hase.soprafs26.entity.TripMembership;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.rest.dto.JoinTripRequestDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.TripPostDTO;
import ch.uzh.ifi.hase.soprafs26.service.DestinationRealtimeService;
import ch.uzh.ifi.hase.soprafs26.service.FinalReportService;
import ch.uzh.ifi.hase.soprafs26.service.TripService;
import ch.uzh.ifi.hase.soprafs26.service.UserService;
import tools.jackson.databind.ObjectMapper;

/**
 * TripControllerTest
 * This is a WebMvcTest which allows to test the TripController i.e. GET/POST
 * request without actually sending them over the network.
 * This tests if the TripController works.
 */
@WebMvcTest(TripController.class)
public class TripControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TripService tripService;

    @MockitoBean
        @SuppressWarnings("unused")
    private DestinationRealtimeService destinationRealtimeService;

        @MockitoBean
        @SuppressWarnings("unused")
        private FinalReportService finalReportService;
  
    @MockitoBean
    private UserService userService;

    private User authenticatedUser() {
        User user = new User();
        user.setId(1L);
        user.setUsername("testUser");
        user.setStatus(UserStatus.ONLINE);
        user.setToken("1");
        return user;
    }

    @Test
    public void givenTrips_whenGetTrips_thenReturnJsonArray() throws Exception {
        // given
        Trip trip = new Trip();
        trip.setId(1L);
        trip.setName("Paris Vacation");
        trip.setRoomCode("ABC123");
        trip.setHostId(1L);
        trip.setStatus(Trip.TripStatus.ACTIVE);

        List<Trip> allTrips = List.of(trip);

        // this mocks the TripService -> we define above what the tripService should
        // return when getAllTrips() is called
        given(userService.validateToken("Bearer 1")).willReturn(authenticatedUser());
        given(tripService.getTripsForUser(1L)).willReturn(allTrips);

        // when
        MockHttpServletRequestBuilder getRequest = get("/trips")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer 1");

        // then
        mockMvc.perform(getRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is(trip.getName())))
                .andExpect(jsonPath("$[0].roomCode", is(trip.getRoomCode())))
                .andExpect(jsonPath("$[0].hostId", is(Math.toIntExact(trip.getHostId()))))
                .andExpect(jsonPath("$[0].status", is(trip.getStatus().toString())));
    }

    @Test
    public void getTripById_validId_success() throws Exception {
        // given
        Trip trip = new Trip();
        trip.setId(1L);
        trip.setName("Paris Vacation");
        trip.setRoomCode("ABC123");
        trip.setHostId(1L);
        trip.setStatus(Trip.TripStatus.ACTIVE);

        given(userService.validateToken("Bearer 1")).willReturn(authenticatedUser());
        given(tripService.getTripById(1L)).willReturn(trip);
        given(tripService.canFinalizeTrip(trip, 1L)).willReturn(true);

        // when
        MockHttpServletRequestBuilder getRequest = get("/trips/1")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer 1");

        // then
        mockMvc.perform(getRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(Math.toIntExact(trip.getId()))))
                .andExpect(jsonPath("$.name", is(trip.getName())))
                .andExpect(jsonPath("$.roomCode", is(trip.getRoomCode())))
                .andExpect(jsonPath("$.host", is(true)))
                .andExpect(jsonPath("$.evaluationMode", is(false)))
                .andExpect(jsonPath("$.finalized", is(false)))
                .andExpect(jsonPath("$.canFinalizeTrip", is(true)));
    }

    @Test
    public void getTripByRoomCode_validCode_success() throws Exception {
        Trip trip = new Trip();
        trip.setId(1L);
        trip.setName("Paris Vacation");
        trip.setRoomCode("ABC123");
        trip.setHostId(1L);
        trip.setStatus(Trip.TripStatus.ACTIVE);

        given(userService.validateToken("Bearer 1")).willReturn(authenticatedUser());
        given(tripService.getTripByRoomCode("ABC123")).willReturn(trip);
        given(tripService.canFinalizeTrip(trip, 1L)).willReturn(true);

        MockHttpServletRequestBuilder getRequest = get("/trips/room/ABC123")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer 1");

        mockMvc.perform(getRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.roomCode", is("ABC123")))
                .andExpect(jsonPath("$.name", is("Paris Vacation")));
    }
    
    @Test
    public void getTripsByHostId_validHostId_success() throws Exception {
        // given
        Trip trip1 = new Trip();
        trip1.setId(1L);
        trip1.setName("Paris Vacation");
        trip1.setRoomCode("ABC123");
        trip1.setHostId(1L);
        trip1.setStatus(Trip.TripStatus.ACTIVE);

        Trip trip2 = new Trip();
        trip2.setId(2L);
        trip2.setName("London Trip");
        trip2.setRoomCode("DEF456");
        trip2.setHostId(1L);
        trip2.setStatus(Trip.TripStatus.ACTIVE);

        List<Trip> trips = Arrays.asList(trip1, trip2);
        given(userService.validateToken("Bearer 1")).willReturn(authenticatedUser());
        given(tripService.getTripsByHostId(1L)).willReturn(trips);

        // when
        MockHttpServletRequestBuilder getRequest = get("/trips/host/1")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer 1");

        // then
        mockMvc.perform(getRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name", is("Paris Vacation")))
                .andExpect(jsonPath("$[1].name", is("London Trip")));
    }

    @Test
    public void createTrip_validInput_tripCreated() throws Exception {
        // given
        TripPostDTO tripPostDTO = new TripPostDTO();
        tripPostDTO.setName("Paris Vacation");

                User authenticatedUser = new User();
                authenticatedUser.setId(1L);

        Trip trip = new Trip();
        trip.setId(1L);
        trip.setName("Paris Vacation");
        trip.setRoomCode("ABC123");
        trip.setHostId(1L);
        trip.setStatus(Trip.TripStatus.ACTIVE);
        trip.setCreationDate(new Date());

        given(userService.validateToken("Bearer test-token")).willReturn(authenticatedUser);
        given(tripService.createTrip(Mockito.any())).willReturn(trip);

        // when
        MockHttpServletRequestBuilder postRequest = post("/trips")
                .header("Authorization", "Bearer test-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(tripPostDTO));

        // then
        mockMvc.perform(postRequest)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(Math.toIntExact(trip.getId()))))
                .andExpect(jsonPath("$.name", is(trip.getName())))
                .andExpect(jsonPath("$.roomCode", is(trip.getRoomCode())))
                .andExpect(jsonPath("$.hostId", is(Math.toIntExact(trip.getHostId()))));
    }

    @Test
    public void createTrip_withImageBase64_returnsImageInResponse() throws Exception {
        TripPostDTO tripPostDTO = new TripPostDTO();
        tripPostDTO.setName("Beach Trip");
        tripPostDTO.setImageBase64("data:image/jpeg;base64,/9j/abc123");

        User authenticatedUser = new User();
        authenticatedUser.setId(1L);

        Trip trip = new Trip();
        trip.setId(2L);
        trip.setName("Beach Trip");
        trip.setRoomCode("XYZ789");
        trip.setHostId(1L);
        trip.setStatus(Trip.TripStatus.ACTIVE);
        trip.setCreationDate(new Date());
        trip.setImageBase64("data:image/jpeg;base64,/9j/abc123");

        given(userService.validateToken("Bearer test-token")).willReturn(authenticatedUser);
        given(tripService.createTrip(Mockito.any())).willReturn(trip);

        MockHttpServletRequestBuilder postRequest = post("/trips")
                .header("Authorization", "Bearer test-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(tripPostDTO));

        mockMvc.perform(postRequest)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.imageBase64", is("data:image/jpeg;base64,/9j/abc123")));
    }

    @Test
    public void updateTripStatus_validInput_success() throws Exception {
        // given
        Trip trip = new Trip();
        trip.setId(1L);
        trip.setName("Paris Vacation");
        trip.setRoomCode("ABC123");
        trip.setHostId(1L);
        trip.setStatus(Trip.TripStatus.EVALUATION);

        given(userService.validateToken("Bearer 1")).willReturn(authenticatedUser());
        given(tripService.startFinalizeTrip(1L, 1L)).willReturn(trip);
        given(tripService.canFinalizeTrip(trip, 1L)).willReturn(false);

        // when
        MockHttpServletRequestBuilder putRequest = put("/trips/1/status")
                .param("newStatus", "EVALUATION")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer 1");

        // then
        mockMvc.perform(putRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("EVALUATION")));
    }

    @Test
    public void updateTripStatus_nonHost_forbidden() throws Exception {
        // given
        User nonHost = new User();
        nonHost.setId(2L);
        nonHost.setUsername("nonHostUser");

        Trip trip = new Trip();
        trip.setId(1L);
        trip.setName("Paris Vacation");
        trip.setRoomCode("ABC123");
        trip.setHostId(1L);
        trip.setStatus(Trip.TripStatus.ACTIVE);

        given(userService.validateToken("Bearer valid-token")).willReturn(nonHost);
        given(tripService.startFinalizeTrip(1L, 2L))
                .willThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the host can finalize the trip"));

        // when
        MockHttpServletRequestBuilder putRequest = put("/trips/1/status")
                .param("newStatus", "EVALUATION")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer valid-token");

        // then
        mockMvc.perform(putRequest)
                .andExpect(status().isForbidden());
    }

    @Test
    public void startFinalizeTrip_validInput_success() throws Exception {
        Trip trip = new Trip();
        trip.setId(1L);
        trip.setName("Paris Vacation");
        trip.setRoomCode("ABC123");
        trip.setHostId(1L);
        trip.setStatus(Trip.TripStatus.EVALUATION);

        given(userService.validateToken("Bearer 1")).willReturn(authenticatedUser());
        given(tripService.startFinalizeTrip(1L, 1L)).willReturn(trip);

        MockHttpServletRequestBuilder postRequest = post("/trips/1/finalize-trip")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer 1");

        mockMvc.perform(postRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("EVALUATION")))
                .andExpect(jsonPath("$.evaluationMode", is(true)))
                .andExpect(jsonPath("$.canFinalizeTrip", is(false)));
    }

    @Test
    public void startFinalizeTrip_nonHost_forbidden() throws Exception {
        User nonHost = new User();
        nonHost.setId(2L);

        given(userService.validateToken("Bearer valid-token")).willReturn(nonHost);
        given(tripService.startFinalizeTrip(1L, 2L))
                .willThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the host can finalize the trip"));

        MockHttpServletRequestBuilder postRequest = post("/trips/1/finalize-trip")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer valid-token");

        mockMvc.perform(postRequest)
                .andExpect(status().isForbidden());
    }

    @Test
    public void setFinalDestination_validInput_success() throws Exception {
        // given
        Trip trip = new Trip();
        trip.setId(1L);
        trip.setName("Paris Vacation");
        trip.setRoomCode("ABC123");
        trip.setHostId(1L);
        trip.setStatus(Trip.TripStatus.FINALIZED);
        trip.setFinalDestinationId(5L);

        given(userService.validateToken("Bearer 1")).willReturn(authenticatedUser());
        given(tripService.setFinalDestination(1L, 5L)).willReturn(trip);

        // when
        MockHttpServletRequestBuilder putRequest = put("/trips/1/finalize")
                .param("finalDestinationId", "5")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer 1");

        // then
        mockMvc.perform(putRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.finalDestinationId", is(5)))
                .andExpect(jsonPath("$.status", is("FINALIZED")));
    }

    @Test
    public void joinTrip_validRequest_success() throws Exception {
        Trip trip = new Trip();
        trip.setId(1L);
        trip.setName("Paris Vacation");
        trip.setRoomCode("ABC123");
        trip.setHostId(1L);
        trip.setStatus(Trip.TripStatus.ACTIVE);

        JoinTripRequestDTO requestDTO = new JoinTripRequestDTO();
        requestDTO.setRoomCode("ABC123");

        given(userService.validateToken("Bearer 1")).willReturn(authenticatedUser());
        given(tripService.joinTripByRoomCode("ABC123", 1L)).willReturn(trip);

        MockHttpServletRequestBuilder postRequest = post("/trips/join")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer 1")
                .content(new ObjectMapper().writeValueAsString(requestDTO));

        mockMvc.perform(postRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tripId", is(1)))
                .andExpect(jsonPath("$.roomCode", is("ABC123")))
                .andExpect(jsonPath("$.userId", is(1)));
    }

    @Test
    public void joinTrip_badRequest_propagatesError() throws Exception {
        JoinTripRequestDTO requestDTO = new JoinTripRequestDTO();
        requestDTO.setRoomCode("   ");

        given(userService.validateToken("Bearer 1")).willReturn(authenticatedUser());
        given(tripService.joinTripByRoomCode("   ", 1L))
                .willThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Room code cannot be empty"));

        MockHttpServletRequestBuilder postRequest = post("/trips/join")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer 1")
                .content(new ObjectMapper().writeValueAsString(requestDTO));

        mockMvc.perform(postRequest)
                .andExpect(status().isBadRequest());
    }

    @Test
    public void getTripParticipants_validRequest_success() throws Exception {
        User aliceUser = new User();
        aliceUser.setId(2L);
        aliceUser.setUsername("alice");

        given(userService.validateToken("Bearer 1")).willReturn(authenticatedUser());
        given(userService.getUserById(1L)).willReturn(authenticatedUser());
        given(userService.getUserById(2L)).willReturn(aliceUser);
        TripMembership host = new TripMembership(1L, 1L);
        TripMembership guest = new TripMembership(1L, 2L);
        given(tripService.getTripParticipants(1L, 1L)).willReturn(List.of(host, guest));

        MockHttpServletRequestBuilder getRequest = get("/trips/1/participants")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer 1");

        mockMvc.perform(getRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].userId", is(1)))
                .andExpect(jsonPath("$[0].username", is("testUser")))
                .andExpect(jsonPath("$[1].userId", is(2)))
                .andExpect(jsonPath("$[1].username", is("alice")));
    }

    @Test
    public void getTripParticipants_guestRequester_returnsHostUsername() throws Exception {
        User guestRequester = new User();
        guestRequester.setId(2L);
        guestRequester.setUsername("guestUser");

        User hostUser = new User();
        hostUser.setId(1L);
        hostUser.setUsername("hostUser");

        given(userService.validateToken("Bearer 2")).willReturn(guestRequester);
        given(userService.getUserById(1L)).willReturn(hostUser);
        given(userService.getUserById(2L)).willReturn(guestRequester);
        given(tripService.getTripParticipants(1L, 2L)).willReturn(List.of(
                new TripMembership(1L, 1L),
                new TripMembership(1L, 2L)
        ));

        MockHttpServletRequestBuilder getRequest = get("/trips/1/participants")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer 2");

        mockMvc.perform(getRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId", is(1)))
                .andExpect(jsonPath("$[0].username", is("hostUser")));
    }

    @Test
    public void generateInvite_validHostToken_success() throws Exception {
        // given
        User hostUser = new User();
        hostUser.setId(1L);
        hostUser.setUsername("hostUser");

        Trip trip = new Trip();
        trip.setId(1L);
        trip.setName("Paris Vacation");
        trip.setRoomCode("ABC123");
        trip.setHostId(1L);
        trip.setStatus(Trip.TripStatus.ACTIVE);
      

        given(userService.validateToken("Bearer valid-token")).willReturn(hostUser);
        given(tripService.getTripById(1L)).willReturn(trip);

        // when
        MockHttpServletRequestBuilder postRequest = post("/trips/1/invite")
                .header("Authorization", "Bearer valid-token")
                .contentType(MediaType.APPLICATION_JSON);

        // then
        mockMvc.perform(postRequest)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.roomCode", is("ABC123")));
    }

        @Test
        public void generateInvite_invalidToken_unauthorized() throws Exception {
        // given
        given(userService.validateToken("Bearer invalid-token"))
                .willThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired token!"));

        // when
        MockHttpServletRequestBuilder postRequest = post("/trips/1/invite")
                .header("Authorization", "Bearer invalid-token")
                .contentType(MediaType.APPLICATION_JSON);

        // then
        mockMvc.perform(postRequest)
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void generateInvite_userNotHost_forbidden() throws Exception {
        // given
        User nonHostUser = new User();
        nonHostUser.setId(2L);
        nonHostUser.setUsername("nonHostUser");

        Trip trip = new Trip();
        trip.setId(1L);
        trip.setName("Paris Vacation");
        trip.setRoomCode("ABC123");
        trip.setHostId(1L); // Different from nonHostUser.getId()
        trip.setStatus(Trip.TripStatus.ACTIVE);

        given(userService.validateToken("Bearer valid-token")).willReturn(nonHostUser);
        given(tripService.getTripById(1L)).willReturn(trip);

        // when
        MockHttpServletRequestBuilder postRequest = post("/trips/1/invite")
                .header("Authorization", "Bearer valid-token")
                .contentType(MediaType.APPLICATION_JSON);

        // then
        mockMvc.perform(postRequest)
                .andExpect(status().isForbidden());
    }
}
