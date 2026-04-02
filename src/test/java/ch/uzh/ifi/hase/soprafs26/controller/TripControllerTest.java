package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.entity.Destination;
import ch.uzh.ifi.hase.soprafs26.entity.Trip;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.rest.dto.DestinationPostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.TripPostDTO;
import ch.uzh.ifi.hase.soprafs26.service.DestinationRealtimeService;
import ch.uzh.ifi.hase.soprafs26.service.TripService;
import ch.uzh.ifi.hase.soprafs26.service.UserService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
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
        given(tripService.getAllTrips()).willReturn(allTrips);

        // when
        MockHttpServletRequestBuilder getRequest = get("/trips")
                .contentType(MediaType.APPLICATION_JSON);

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

        given(tripService.getTripById(1L)).willReturn(trip);

        // when
        MockHttpServletRequestBuilder getRequest = get("/trips/1")
                .contentType(MediaType.APPLICATION_JSON);

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

        given(tripService.getTripByRoomCode("ABC123")).willReturn(trip);

        // when
        MockHttpServletRequestBuilder getRequest = get("/trips/room/ABC123")
                .contentType(MediaType.APPLICATION_JSON);

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
        given(tripService.getTripsByHostId(1L)).willReturn(trips);

        // when
        MockHttpServletRequestBuilder getRequest = get("/trips/host/1")
                .contentType(MediaType.APPLICATION_JSON);

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

        Trip trip = new Trip();
        trip.setId(1L);
        trip.setName("Paris Vacation");
        trip.setRoomCode("ABC123");
        trip.setHostId(1L);
        trip.setStatus(Trip.TripStatus.ACTIVE);
        trip.setCreationDate(new Date());

        given(tripService.createTrip(Mockito.any())).willReturn(trip);

        // when
        MockHttpServletRequestBuilder postRequest = post("/trips")
                .param("hostId", "1")
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

        given(tripService.updateTripStatus(1L, Trip.TripStatus.EVALUATION)).willReturn(trip);

        // when
        MockHttpServletRequestBuilder putRequest = put("/trips/1/status")
                .param("newStatus", "EVALUATION")
                .contentType(MediaType.APPLICATION_JSON);

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

        given(tripService.setFinalDestination(1L, 5L)).willReturn(trip);

        // when
        MockHttpServletRequestBuilder putRequest = put("/trips/1/finalize")
                .param("finalDestinationId", "5")
                .contentType(MediaType.APPLICATION_JSON);

        // then
        mockMvc.perform(putRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.finalDestinationId", is(5)))
                .andExpect(jsonPath("$.status", is("FINALIZED")));
    }
}
