package ch.uzh.ifi.hase.soprafs26.controller;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.entity.Activity;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.rest.dto.ActivityPostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.ActivitySearchResultDTO;
import ch.uzh.ifi.hase.soprafs26.service.ActivityManagementService;
import ch.uzh.ifi.hase.soprafs26.service.ActivitySearchService;
import ch.uzh.ifi.hase.soprafs26.service.UserService;
import tools.jackson.databind.ObjectMapper;

public class ActivityControllerTest {

    @Mock
    private ActivitySearchService activitySearchService;

    @Mock
    private ActivityManagementService activityManagementService;

    @Mock
    private UserService userService;

    private ActivityController activityController;

    private MockMvc mockMvc;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        activityController = new ActivityController(activitySearchService, activityManagementService, userService);
        mockMvc = MockMvcBuilders.standaloneSetup(activityController).build();
    }

    @Test
    public void searchActivities_validQuery_success() {
        User user = new User();
        user.setId(1L);

        ActivitySearchResultDTO resultDTO = new ActivitySearchResultDTO();
        resultDTO.setPlaceId("place-1");
        resultDTO.setName("City Museum");
        resultDTO.setAddress("Main Street 1");
        resultDTO.setRating(4.6);

        Mockito.when(userService.validateToken("Bearer token")).thenReturn(user);
        Mockito.when(activitySearchService.searchActivities(1L, 2L, "museum", "Zurich", 2000))
                .thenReturn(List.of(resultDTO));

        try {
            mockMvc.perform(get("/trips/1/destinations/2/activities")
                    .header("Authorization", "Bearer token")
                            .param("query", "museum")
                            .param("location", "Zurich")
                            .param("radius", "2000")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].placeId").value("place-1"))
                    .andExpect(jsonPath("$[0].name").value("City Museum"))
                    .andExpect(jsonPath("$[0].address").value("Main Street 1"))
                    .andExpect(jsonPath("$[0].rating").value(4.6));
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    @Test
    public void getSavedActivities_withoutQuery_success() {
        User user = new User();
        user.setId(1L);

        Activity activity = new Activity();
        activity.setId(10L);
        activity.setPlaceId("saved-place");
        activity.setName("Saved Place");

        Mockito.when(userService.validateToken("Bearer token")).thenReturn(user);
        Mockito.when(activityManagementService.getSelectedActivities(1L, 2L)).thenReturn(List.of(activity));

        try {
            mockMvc.perform(get("/trips/1/destinations/2/activities")
                            .header("Authorization", "Bearer token")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].placeId").value("saved-place"));
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    @Test
    public void addActivity_validPayload_success() {
        User user = new User();
        user.setId(1L);

        ActivityPostDTO postDTO = new ActivityPostDTO();
        postDTO.setPlaceId("place-1");
        postDTO.setName("City Museum");
        postDTO.setAddress("Main Street 1");

        Activity saved = new Activity();
        saved.setId(10L);
        saved.setPlaceId("place-1");
        saved.setName("City Museum");

        Mockito.when(userService.validateToken("Bearer token")).thenReturn(user);
        Mockito.when(activityManagementService.addActivity(Mockito.eq(1L), Mockito.eq(2L), Mockito.eq(1L), Mockito.any(Activity.class)))
                .thenReturn(saved);

        try {
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/trips/1/destinations/2/activities")
                            .header("Authorization", "Bearer token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(new ObjectMapper().writeValueAsString(postDTO)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.placeId").value("place-1"))
                    .andExpect(jsonPath("$.name").value("City Museum"));
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    @Test
    public void updateActivity_success() {
        User user = new User();
        user.setId(1L);

        ActivityPostDTO postDTO = new ActivityPostDTO();
        postDTO.setPlaceId("place-1");
        postDTO.setName("Updated Museum");

        Activity updated = new Activity();
        updated.setId(10L);
        updated.setPlaceId("place-1");
        updated.setName("Updated Museum");

        Mockito.when(userService.validateToken("Bearer token")).thenReturn(user);
        Mockito.when(activityManagementService.updateActivity(Mockito.eq(1L), Mockito.eq(2L), Mockito.eq(10L), Mockito.eq(1L), Mockito.any(Activity.class)))
                .thenReturn(updated);

        try {
            mockMvc.perform(put("/trips/1/destinations/2/activities/10")
                            .header("Authorization", "Bearer token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(new ObjectMapper().writeValueAsString(postDTO)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(10))
                    .andExpect(jsonPath("$.name").value("Updated Museum"));
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    @Test
    public void deleteActivity_success() {
        User user = new User();
        user.setId(1L);

        Mockito.when(userService.validateToken("Bearer token")).thenReturn(user);

        try {
            mockMvc.perform(delete("/trips/1/destinations/2/activities/10")
                            .header("Authorization", "Bearer token")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNoContent());
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    // #222
    @Test
    public void deleteActivity_notCreator_returns403() {
        User user = new User();
        user.setId(1L);

        Mockito.when(userService.validateToken("Bearer token")).thenReturn(user);
        Mockito.doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the creator can delete this activity"))
                .when(activityManagementService).deleteActivity(1L, 2L, 10L, 1L);

        try {
            mockMvc.perform(delete("/trips/1/destinations/2/activities/10")
                            .header("Authorization", "Bearer token")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden());
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    // #223
    @Test
    public void deleteActivity_hasUpvotes_returns400() {
        User user = new User();
        user.setId(1L);

        Mockito.when(userService.validateToken("Bearer token")).thenReturn(user);
        Mockito.doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot delete activity with existing votes"))
                .when(activityManagementService).deleteActivity(1L, 2L, 10L, 1L);

        try {
            mockMvc.perform(delete("/trips/1/destinations/2/activities/10")
                            .header("Authorization", "Bearer token")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    // #122
    @Test
    public void getSavedActivities_includesCreatedBy_inResponse() {
        User user = new User();
        user.setId(1L);

        Activity activity = new Activity();
        activity.setId(10L);
        activity.setPlaceId("place-1");
        activity.setName("Museum");
        activity.setCreatedBy(42L);

        Mockito.when(userService.validateToken("Bearer token")).thenReturn(user);
        Mockito.when(activityManagementService.getSelectedActivities(1L, 2L)).thenReturn(List.of(activity));

        try {
            mockMvc.perform(get("/trips/1/destinations/2/activities")
                            .header("Authorization", "Bearer token")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].createdBy").value(42));
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }
}