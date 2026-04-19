package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.entity.Activity;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.rest.dto.ActivityCommentRequestDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.ActivityPostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.ActivitySearchResultDTO;
import ch.uzh.ifi.hase.soprafs26.service.ActivityManagementService;
import ch.uzh.ifi.hase.soprafs26.service.ActivitySearchService;
import ch.uzh.ifi.hase.soprafs26.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
        activity.setComment("Looks promising");

        Mockito.when(userService.validateToken("Bearer token")).thenReturn(user);
        Mockito.when(activityManagementService.getSelectedActivities(1L, 2L)).thenReturn(List.of(activity));

        try {
            mockMvc.perform(get("/trips/1/destinations/2/activities")
                            .header("Authorization", "Bearer token")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].placeId").value("saved-place"))
                    .andExpect(jsonPath("$[0].comment").value("Looks promising"));
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
        postDTO.setComment("Great backup option");

        Activity saved = new Activity();
        saved.setId(10L);
        saved.setPlaceId("place-1");
        saved.setName("City Museum");
        saved.setComment("Great backup option");

        Mockito.when(userService.validateToken("Bearer token")).thenReturn(user);
        Mockito.when(activityManagementService.addActivity(Mockito.eq(1L), Mockito.eq(2L), Mockito.any(Activity.class)))
                .thenReturn(saved);

        try {
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/trips/1/destinations/2/activities")
                            .header("Authorization", "Bearer token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(new ObjectMapper().writeValueAsString(postDTO)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.placeId").value("place-1"))
                    .andExpect(jsonPath("$.name").value("City Museum"))
                    .andExpect(jsonPath("$.comment").value("Great backup option"));
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
        postDTO.setComment("Now with better opening hours");

        Activity updated = new Activity();
        updated.setId(10L);
        updated.setPlaceId("place-1");
        updated.setName("Updated Museum");
        updated.setComment("Now with better opening hours");

        Mockito.when(userService.validateToken("Bearer token")).thenReturn(user);
        Mockito.when(activityManagementService.updateActivity(Mockito.eq(1L), Mockito.eq(2L), Mockito.eq(10L), Mockito.any(Activity.class)))
                .thenReturn(updated);

        try {
            mockMvc.perform(put("/trips/1/destinations/2/activities/10")
                            .header("Authorization", "Bearer token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(new ObjectMapper().writeValueAsString(postDTO)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(10))
                    .andExpect(jsonPath("$.name").value("Updated Museum"))
                    .andExpect(jsonPath("$.comment").value("Now with better opening hours"));
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    @Test
    public void updateActivityComment_success() {
        User user = new User();
        user.setId(1L);

        ActivityCommentRequestDTO requestDTO = new ActivityCommentRequestDTO();
        requestDTO.setComment("This is a must-do activity");

        Activity updated = new Activity();
        updated.setId(10L);
        updated.setPlaceId("place-1");
        updated.setName("Updated Museum");
        updated.setComment("This is a must-do activity");

        Mockito.when(userService.validateToken("Bearer token")).thenReturn(user);
        Mockito.when(activityManagementService.updateActivityComment(Mockito.eq(10L), Mockito.eq(1L), Mockito.any(ActivityCommentRequestDTO.class)))
                .thenReturn(updated);

        try {
            mockMvc.perform(put("/activities/10/comment")
                            .header("Authorization", "Bearer token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(new ObjectMapper().writeValueAsString(requestDTO)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(10))
                    .andExpect(jsonPath("$.comment").value("This is a must-do activity"));
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
}