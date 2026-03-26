package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.constant.TripStatus;
import ch.uzh.ifi.hase.soprafs26.entity.Trip;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.service.TripService;
import ch.uzh.ifi.hase.soprafs26.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TripController.class)
public class TripControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TripService tripService;

    @MockitoBean
    private UserService userService;

    @Test
    public void givenTrips_whenGetTripsWithValidToken_thenReturnJsonArray() throws Exception {
        Trip trip = new Trip();
        trip.setName("Spring Hike");
        trip.setHostId(1L);
        trip.setRoomCode("ABCD1234");
        trip.setStatus(TripStatus.PLANNING);
        trip.initializeDefaults();

        List<Trip> allTrips = Collections.singletonList(trip);

        given(tripService.getTrips()).willReturn(allTrips);
                given(userService.validateToken(anyString())).willReturn(new User());

        MockHttpServletRequestBuilder getRequest = get("/trips")
                .header("Authorization", "Bearer valid-token")
                .contentType(MediaType.APPLICATION_JSON);

        mockMvc.perform(getRequest).andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].tripName", is(trip.getName())))
                .andExpect(jsonPath("$[0].status", is(trip.getStatus().toString())));
    }

    @Test
    public void givenNoToken_whenGetTrips_thenReturnUnauthorized() throws Exception {
        willThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authorization token is required!"))
                .given(userService)
                .validateToken(null);

        MockHttpServletRequestBuilder getRequest = get("/trips")
                .contentType(MediaType.APPLICATION_JSON);

        mockMvc.perform(getRequest).andExpect(status().isUnauthorized());
    }
}
