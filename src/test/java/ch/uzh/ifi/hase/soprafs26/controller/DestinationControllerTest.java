package ch.uzh.ifi.hase.soprafs26.controller;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import org.mockito.Mockito;
import static org.mockito.Mockito.verify;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import ch.uzh.ifi.hase.soprafs26.entity.Activity;
import ch.uzh.ifi.hase.soprafs26.entity.Destination;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.event.DestinationVotesUpdatedEvent;
import ch.uzh.ifi.hase.soprafs26.event.TripStatusUpdatedEvent;
import ch.uzh.ifi.hase.soprafs26.rest.dto.DestinationPostDTO;
import ch.uzh.ifi.hase.soprafs26.service.ActivityManagementService;
import ch.uzh.ifi.hase.soprafs26.service.DestinationRealtimeService;
import ch.uzh.ifi.hase.soprafs26.service.DestinationService;
import ch.uzh.ifi.hase.soprafs26.service.TripService;
import ch.uzh.ifi.hase.soprafs26.service.UserService;
import tools.jackson.databind.ObjectMapper;

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
        Mockito.when(tripService.addDestination(eq(1L), eq(1L), any(Destination.class))).thenReturn(saved);
        Mockito.when(tripService.getDestinations(1L, 1L)).thenReturn(List.of(saved));
        Mockito.when(activityManagementService.getSelectedActivities(1L, 11L)).thenReturn(List.of());

        mockMvc.perform(post("/trips/1/destinations")
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(postDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(11))
                .andExpect(jsonPath("$.destinationName").value("Zurich"));

        verify(destinationRealtimeService).publish(eq(1L), any(List.class));
    }

    @Test
    public void createDestination_nonParticipant_forbidden() throws Exception {
        User user = new User();
        user.setId(99L);

        DestinationPostDTO postDTO = new DestinationPostDTO();
        postDTO.setDestinationName("Bern");

        Mockito.when(userService.validateToken("Bearer token")).thenReturn(user);
        Mockito.when(tripService.addDestination(eq(1L), eq(99L), any(Destination.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "User is not a participant of this trip"));

        mockMvc.perform(post("/trips/1/destinations")
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(postDTO)))
                .andExpect(status().isForbidden());
    }

    @Test
    public void createDestination_duplicateName_conflict() throws Exception {
        User user = new User();
        user.setId(1L);

        DestinationPostDTO postDTO = new DestinationPostDTO();
        postDTO.setDestinationName("Zurich");

        Mockito.when(userService.validateToken("Bearer token")).thenReturn(user);
        Mockito.when(tripService.addDestination(eq(1L), eq(1L), any(Destination.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT,
                        "Destination with this name already exists for this trip"));

        mockMvc.perform(post("/trips/1/destinations")
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(postDTO)))
                .andExpect(status().isConflict());
    }

    @Test
    public void createDestination_emptyName_badRequest() throws Exception {
        User user = new User();
        user.setId(1L);

        DestinationPostDTO postDTO = new DestinationPostDTO();
        postDTO.setDestinationName("   ");

        Mockito.when(userService.validateToken("Bearer token")).thenReturn(user);
        Mockito.when(tripService.addDestination(eq(1L), eq(1L), any(Destination.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Destination name cannot be empty"));

        mockMvc.perform(post("/trips/1/destinations")
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(postDTO)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void createDestination_inactiveTrip_badRequest() throws Exception {
        User user = new User();
        user.setId(1L);

        DestinationPostDTO postDTO = new DestinationPostDTO();
        postDTO.setDestinationName("Basel");

        Mockito.when(userService.validateToken("Bearer token")).thenReturn(user);
        Mockito.when(tripService.addDestination(eq(1L), eq(1L), any(Destination.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Trip is in read-only mode. Mutations are only allowed while trip is ACTIVE"));

        mockMvc.perform(post("/trips/1/destinations")
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(postDTO)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void streamDestinations_success() {
        User user = new User();
        user.setId(1L);
        SseEmitter emitter = new SseEmitter(0L);

        Mockito.when(userService.validateToken("Bearer token")).thenReturn(user);
        Mockito.when(tripService.getDestinations(1L, 1L)).thenReturn(List.of());
        Mockito.when(destinationRealtimeService.subscribe(1L)).thenReturn(emitter);

        DestinationController destinationController = new DestinationController(
                destinationService,
                tripService,
                activityManagementService,
                userService,
                destinationRealtimeService);
        MockHttpServletResponse response = new MockHttpServletResponse();

        SseEmitter result = destinationController.streamDestinations(1L, "Bearer token", response);

        assertNotNull(result);
        assertEquals(emitter, result);
        assertEquals(MediaType.TEXT_EVENT_STREAM_VALUE, response.getContentType());
        assertEquals("no-cache, no-transform", response.getHeader("Cache-Control"));
        assertEquals("no", response.getHeader("X-Accel-Buffering"));
    }

    @Test
    public void updateDestination_success() throws Exception {
        User user = new User();
        user.setId(1L);

        DestinationPostDTO postDTO = new DestinationPostDTO();
        postDTO.setDestinationName("Geneva");

        Destination saved = new Destination();
        saved.setId(11L);
        saved.setTripId(1L);
        saved.setDestinationName("Geneva");
        saved.setProposedByUserId(1L);

        Mockito.when(userService.validateToken("Bearer token")).thenReturn(user);
        Mockito.when(destinationService.updateDestination(eq(1L), eq(11L), any(Destination.class), eq(1L))).thenReturn(saved);
        Mockito.when(tripService.getDestinations(1L, 1L)).thenReturn(List.of(saved));
        Mockito.when(activityManagementService.getSelectedActivities(1L, 11L)).thenReturn(List.of());

        mockMvc.perform(put("/trips/1/destinations/11")
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(postDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(11))
                .andExpect(jsonPath("$.destinationName").value("Geneva"));

        verify(destinationRealtimeService).publish(eq(1L), any(List.class));
    }

    @Test
    public void updateDestination_withNullActivityItem_returnsEmptyActivityDto() throws Exception {
        User user = new User();
        user.setId(1L);

        DestinationPostDTO postDTO = new DestinationPostDTO();
        postDTO.setDestinationName("Geneva");

        Destination saved = new Destination();
        saved.setId(11L);
        saved.setTripId(1L);
        saved.setDestinationName("Geneva");
        saved.setProposedByUserId(1L);

        Mockito.when(userService.validateToken("Bearer token")).thenReturn(user);
        Mockito.when(destinationService.updateDestination(eq(1L), eq(11L), any(Destination.class), eq(1L))).thenReturn(saved);
        Mockito.when(tripService.getDestinations(1L, 1L)).thenReturn(List.of(saved));
        Mockito.when(activityManagementService.getSelectedActivities(1L, 11L)).thenReturn(Arrays.asList((Activity) null));

        mockMvc.perform(put("/trips/1/destinations/11")
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(postDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activities[0].id").doesNotExist())
                .andExpect(jsonPath("$.activities[0].name").doesNotExist());
    }

    @Test
    public void updateDestination_withActivity_mapsCreatedBy() throws Exception {
        User user = new User();
        user.setId(1L);

        DestinationPostDTO postDTO = new DestinationPostDTO();
        postDTO.setDestinationName("Geneva");

        Destination saved = new Destination();
        saved.setId(11L);
        saved.setTripId(1L);
        saved.setDestinationName("Geneva");
        saved.setProposedByUserId(1L);

        Activity activity = new Activity();
        activity.setId(5L);
        activity.setTripId(1L);
        activity.setDestinationId(11L);
        activity.setPlaceId("place-1");
        activity.setName("Museum");
        activity.setCreatedBy(7L);

        Mockito.when(userService.validateToken("Bearer token")).thenReturn(user);
        Mockito.when(destinationService.updateDestination(eq(1L), eq(11L), any(Destination.class), eq(1L))).thenReturn(saved);
        Mockito.when(tripService.getDestinations(1L, 1L)).thenReturn(List.of(saved));
        Mockito.when(activityManagementService.getSelectedActivities(1L, 11L)).thenReturn(List.of(activity));

        mockMvc.perform(put("/trips/1/destinations/11")
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(postDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activities[0].id").value(5))
                .andExpect(jsonPath("$.activities[0].createdBy").value(7));
    }

    @Test
    public void updateDestination_forbiddenWhenActivitiesExist_conflict() throws Exception {
        User user = new User();
        user.setId(1L);

        DestinationPostDTO postDTO = new DestinationPostDTO();
        postDTO.setDestinationName("Bern");

        Mockito.when(userService.validateToken("Bearer token")).thenReturn(user);
        Mockito.when(destinationService.updateDestination(eq(1L), eq(11L), any(Destination.class), eq(1L)))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Destination already has activities and cannot be edited"));

        mockMvc.perform(put("/trips/1/destinations/11")
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(postDTO)))
                .andExpect(status().isConflict());
    }

    @Test
    public void deleteDestination_success() throws Exception {
        User user = new User();
        user.setId(1L);

        Mockito.when(userService.validateToken("Bearer token")).thenReturn(user);

        mockMvc.perform(delete("/trips/1/destinations/11")
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isNoContent());

        verify(destinationService).deleteDestination(1L, 11L);
    }

    @Test
    public void handleDestinationVotesUpdated_publishesSharedList() {
        Destination destination = new Destination();
        destination.setId(11L);
        destination.setTripId(1L);
        destination.setDestinationName("Zurich");
        destination.setProposedByUserId(1L);

        Mockito.when(tripService.getDestinations(1L, 2L)).thenReturn(List.of(destination));
        Mockito.when(activityManagementService.getSelectedActivities(1L, 11L)).thenReturn(List.of());

        DestinationController destinationController = new DestinationController(
                destinationService,
                tripService,
                activityManagementService,
                userService,
                destinationRealtimeService);

        destinationController.handleDestinationVotesUpdated(new DestinationVotesUpdatedEvent(this, 1L, 2L));

        verify(destinationRealtimeService).publish(eq(1L), any(List.class));
    }

    @Test
    public void handleDestinationVotesUpdated_withNullRealtimeService_doesNothing() {
        DestinationController destinationController = new DestinationController(
                destinationService,
                tripService,
                activityManagementService,
                userService,
                null);

        assertDoesNotThrow(() -> destinationController.handleDestinationVotesUpdated(new DestinationVotesUpdatedEvent(this, 1L, 2L)));
    }

    @Test
    public void handleTripStatusUpdated_publishesStatusEvent() {
        DestinationController destinationController = new DestinationController(
                destinationService,
                tripService,
                activityManagementService,
                userService,
                destinationRealtimeService);

        destinationController.handleTripStatusUpdated(new TripStatusUpdatedEvent(this, 1L, "FINALIZED"));

        verify(destinationRealtimeService).publish(1L, "trip-status-updated", "FINALIZED");
    }

    @Test
    public void handleTripStatusUpdated_withNullRealtimeService_doesNothing() {
        DestinationController destinationController = new DestinationController(
                destinationService,
                tripService,
                activityManagementService,
                userService,
                null);

        assertDoesNotThrow(() -> destinationController.handleTripStatusUpdated(new TripStatusUpdatedEvent(this, 1L, "FINALIZED")));
    }
}
