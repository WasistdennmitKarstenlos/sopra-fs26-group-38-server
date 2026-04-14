package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.entity.Destination;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.rest.dto.DestinationPostDTO;
import ch.uzh.ifi.hase.soprafs26.service.ActivityManagementService;
import ch.uzh.ifi.hase.soprafs26.service.DestinationRealtimeService;
import ch.uzh.ifi.hase.soprafs26.service.DestinationService;
import ch.uzh.ifi.hase.soprafs26.service.TripService;
import ch.uzh.ifi.hase.soprafs26.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class DestinationControllerTest {

    @Mock
    private DestinationService destinationService;

    @Mock
    private TripService tripService;

    @Mock
    private ActivityManagementService activityManagementService;

    @Mock
    private DestinationRealtimeService destinationRealtimeService;

    @Mock
    private UserService userService;

    private MockMvc mockMvc;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        DestinationController destinationController = new DestinationController(
                destinationService,
                tripService,
                activityManagementService,
                userService,
                destinationRealtimeService);
        mockMvc = MockMvcBuilders.standaloneSetup(destinationController).build();
    }

    @Test
    public void getDestinations_success() throws Exception {
        User user = new User();
        user.setId(1L);

        Destination destination = new Destination();
        destination.setId(11L);
        destination.setTripId(1L);
        destination.setDestinationName("Zurich");
        destination.setProposedByUserId(1L);

        Mockito.when(userService.validateToken("Bearer token")).thenReturn(user);
        Mockito.when(tripService.getDestinations(1L, 1L)).thenReturn(List.of(destination));
        Mockito.when(activityManagementService.getSelectedActivities(1L, 11L)).thenReturn(List.of());

        mockMvc.perform(get("/trips/1/destinations")
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(11))
                .andExpect(jsonPath("$[0].destinationName").value("Zurich"));
    }

    @Test
    public void createDestination_success() throws Exception {
        User user = new User();
        user.setId(1L);

        DestinationPostDTO postDTO = new DestinationPostDTO();
        postDTO.setDestinationName("Zurich");

        Destination saved = new Destination();
        saved.setId(11L);
        saved.setTripId(1L);
        saved.setDestinationName("Zurich");
        saved.setProposedByUserId(1L);

        Mockito.when(userService.validateToken("Bearer token")).thenReturn(user);
        Mockito.when(tripService.addDestination(Mockito.eq(1L), Mockito.eq(1L), Mockito.any(Destination.class))).thenReturn(saved);
        Mockito.when(tripService.getDestinations(1L, 1L)).thenReturn(List.of(saved));
        Mockito.when(activityManagementService.getSelectedActivities(1L, 11L)).thenReturn(List.of());

        mockMvc.perform(post("/trips/1/destinations")
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(postDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(11))
                .andExpect(jsonPath("$.destinationName").value("Zurich"));
    }

    @Test
    public void streamDestinations_success() throws Exception {
        User user = new User();
        user.setId(1L);

        Mockito.when(userService.validateToken("Bearer token")).thenReturn(user);
        Mockito.when(tripService.getDestinations(1L, 1L)).thenReturn(List.of());
        Mockito.when(destinationRealtimeService.subscribe(1L)).thenReturn(new SseEmitter(0L));

        mockMvc.perform(get("/trips/1/destinations/stream")
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isOk());
    }
}