package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.entity.User;
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

        ActivitySearchResultDTO resultDTO = new ActivitySearchResultDTO();
        resultDTO.setPlaceId("saved-place");
        resultDTO.setName("Saved Place");

        Mockito.when(userService.validateToken("Bearer token")).thenReturn(user);
        Mockito.when(activityManagementService.getSelectedActivities(1L, 2L)).thenReturn(List.of(resultDTO));

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

        ActivitySearchResultDTO savedDTO = new ActivitySearchResultDTO();
        savedDTO.setPlaceId("place-1");
        savedDTO.setName("City Museum");

        Mockito.when(userService.validateToken("Bearer token")).thenReturn(user);
        Mockito.when(activityManagementService.addActivity(Mockito.eq(1L), Mockito.eq(2L), Mockito.any(ActivityPostDTO.class)))
                .thenReturn(savedDTO);

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
}