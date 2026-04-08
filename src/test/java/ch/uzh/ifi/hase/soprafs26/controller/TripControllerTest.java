package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.entity.Destination;
import ch.uzh.ifi.hase.soprafs26.entity.Trip;
import ch.uzh.ifi.hase.soprafs26.rest.dto.DestinationPostDTO;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs26.rest.dto.TripPostDTO;
import ch.uzh.ifi.hase.soprafs26.service.DestinationRealtimeService;
import ch.uzh.ifi.hase.soprafs26.service.TripService;
import ch.uzh.ifi.hase.soprafs26.service.UserService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import tools.jackson.databind.ObjectMapper;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
    private DestinationRealtimeService destinationRealtimeService;
  
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

        List<Trip> allTrips = Collections.singletonList(trip);

        // this mocks the TripService -> we define above what the tripService should
        // return when getAllTrips() is called
        given(userService.validateToken("Bearer 1")).willReturn(authenticatedUser());
        given(tripService.getAllTrips()).willReturn(allTrips);

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

        // when
        MockHttpServletRequestBuilder getRequest = get("/trips/1")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer 1");

        // then
        mockMvc.perform(getRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(Math.toIntExact(trip.getId()))))
                .andExpect(jsonPath("$.name", is(trip.getName())))
                .andExpect(jsonPath("$.roomCode", is(trip.getRoomCode())));
    }

    @Test
    public void getTripByRoomCode_validCode_success() throws Exception {
        // given
        Trip trip = new Trip();
        trip.setId(1L);
        trip.setName("Paris Vacation");
        trip.setRoomCode("ABC123");
        trip.setHostId(1L);
        trip.setStatus(Trip.TripStatus.ACTIVE);

        given(userService.validateToken("Bearer 1")).willReturn(authenticatedUser());
        given(tripService.getTripByRoomCode("ABC123")).willReturn(trip);

        // when
        MockHttpServletRequestBuilder getRequest = get("/trips/room/ABC123")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer 1");

        // then
        mockMvc.perform(getRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roomCode", is("ABC123")))
                .andExpect(jsonPath("$.name", is(trip.getName())));
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
    public void updateTripStatus_validInput_success() throws Exception {
        // given
        Trip trip = new Trip();
        trip.setId(1L);
        trip.setName("Paris Vacation");
        trip.setRoomCode("ABC123");
        trip.setHostId(1L);
        trip.setStatus(Trip.TripStatus.EVALUATION);

        given(userService.validateToken("Bearer 1")).willReturn(authenticatedUser());
        given(tripService.updateTripStatus(1L, Trip.TripStatus.EVALUATION)).willReturn(trip);

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
    public void addDestination_validInput_created() throws Exception {
        DestinationPostDTO postDTO = new DestinationPostDTO();
        postDTO.setDestinationName("Zurich");

        User requester = new User();
        requester.setId(2L);

        Destination destination = new Destination();
        destination.setId(10L);
        destination.setTripId(1L);
        destination.setDestinationName("Zurich");
        destination.setProposedByUserId(2L);

        given(userService.validateToken("Bearer token-1")).willReturn(requester);
        given(tripService.addDestination(Mockito.eq(1L), Mockito.eq(2L), Mockito.any())).willReturn(destination);
        given(tripService.getDestinations(1L, 2L)).willReturn(Collections.singletonList(destination));

        MockHttpServletRequestBuilder postRequest = post("/trips/1/destinations")
                .header("Authorization", "Bearer token-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(postDTO));

        mockMvc.perform(postRequest)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(10)))
                .andExpect(jsonPath("$.destinationName", is("Zurich")))
                .andExpect(jsonPath("$.tripId", is(1)));

        Mockito.verify(destinationRealtimeService, Mockito.times(1)).publish(Mockito.eq(1L), Mockito.any());
    }

    @Test
    public void getDestinations_validToken_success() throws Exception {
        User requester = new User();
        requester.setId(2L);

        Destination destination = new Destination();
        destination.setId(10L);
        destination.setTripId(1L);
        destination.setDestinationName("Zurich");
        destination.setProposedByUserId(2L);

        given(userService.validateToken("Bearer token-1")).willReturn(requester);
        given(tripService.getDestinations(1L, 2L)).willReturn(Collections.singletonList(destination));

        MockHttpServletRequestBuilder getRequest = get("/trips/1/destinations")
                .header("Authorization", "Bearer token-1")
                .contentType(MediaType.APPLICATION_JSON);

        mockMvc.perform(getRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].destinationName", is("Zurich")))
                .andExpect(jsonPath("$[0].tripId", is(1)));
    }

    @Test
    public void streamDestinations_validToken_success() throws Exception {
        User requester = new User();
        requester.setId(2L);

        given(userService.validateToken("Bearer token-1")).willReturn(requester);
        given(tripService.getDestinations(1L, 2L)).willReturn(Collections.emptyList());
        given(destinationRealtimeService.subscribe(1L)).willReturn(new SseEmitter(0L));

        MockHttpServletRequestBuilder getRequest = get("/trips/1/destinations/stream")
                .header("Authorization", "Bearer token-1");

        mockMvc.perform(getRequest)
                .andExpect(status().isOk());
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
