package ch.uzh.ifi.hase.soprafs26.controller;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.entity.Activity;
import ch.uzh.ifi.hase.soprafs26.entity.Comment;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.rest.dto.ActivityPostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.ActivitySearchResultDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.ActivityVoteRequestDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.ActivityVoteResponseDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.CommentPostDTO;
import ch.uzh.ifi.hase.soprafs26.service.ActivityManagementService;
import ch.uzh.ifi.hase.soprafs26.service.ActivitySearchService;
import ch.uzh.ifi.hase.soprafs26.service.CommentService;
import ch.uzh.ifi.hase.soprafs26.service.UserService;
import tools.jackson.databind.ObjectMapper;

public class ActivityControllerTest {

    @Mock
    private ActivitySearchService activitySearchService;

    @Mock
    private ActivityManagementService activityManagementService;

    @Mock
    private CommentService commentService;

    @Mock
    private UserService userService;

    private ActivityController activityController;

    private MockMvc mockMvc;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        activityController = new ActivityController(activitySearchService, activityManagementService, commentService, userService);
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

    @Test
    public void addComment_validPayload_success() {
        User user = new User();
        user.setId(1L);
        user.setUsername("alice");

        CommentPostDTO postDTO = new CommentPostDTO();
        postDTO.setContent("Looks amazing");

        Comment saved = new Comment();
        saved.setId(99L);
        saved.setTripId(1L);
        saved.setDestinationId(2L);
        saved.setActivityId(10L);
        saved.setUserId(1L);
        saved.setContent("Looks amazing");
        saved.setCreatedAt(java.time.LocalDateTime.of(2026, 5, 3, 12, 0));

        Mockito.when(userService.validateToken("Bearer token")).thenReturn(user);
        Mockito.when(commentService.addComment(1L, 2L, 10L, 1L, "Looks amazing")).thenReturn(saved);
        Mockito.when(userService.getUserById(1L)).thenReturn(user);

        try {
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/trips/1/destinations/2/activities/10/comments")
                            .header("Authorization", "Bearer token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(new ObjectMapper().writeValueAsString(postDTO)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(99))
                    .andExpect(jsonPath("$.userId").value(1))
                    .andExpect(jsonPath("$.username").value("alice"))
                    .andExpect(jsonPath("$.content").value("Looks amazing"));
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    @Test
    public void getComments_success() {
        User user = new User();
        user.setId(1L);
        user.setUsername("alice");

        Comment first = new Comment();
        first.setId(1L);
        first.setTripId(1L);
        first.setDestinationId(2L);
        first.setActivityId(10L);
        first.setUserId(1L);
        first.setContent("First comment");
        first.setCreatedAt(java.time.LocalDateTime.of(2026, 5, 3, 11, 30));

        Mockito.when(userService.validateToken("Bearer token")).thenReturn(user);
        Mockito.when(commentService.getComments(1L, 2L, 10L, 1L)).thenReturn(List.of(first));
        Mockito.when(userService.getUserById(1L)).thenReturn(user);

        try {
            mockMvc.perform(get("/trips/1/destinations/2/activities/10/comments")
                            .header("Authorization", "Bearer token")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(1))
                    .andExpect(jsonPath("$[0].username").value("alice"))
                    .andExpect(jsonPath("$[0].content").value("First comment"));
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    @Test
    public void addComment_emptyContent_returns400() {
        User user = new User();
        user.setId(1L);

        CommentPostDTO postDTO = new CommentPostDTO();
        postDTO.setContent("   ");

        Mockito.when(userService.validateToken("Bearer token")).thenReturn(user);
        Mockito.when(commentService.addComment(1L, 2L, 10L, 1L, "   "))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Comment content cannot be empty"));

        try {
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/trips/1/destinations/2/activities/10/comments")
                            .header("Authorization", "Bearer token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(new ObjectMapper().writeValueAsString(postDTO)))
                    .andExpect(status().isBadRequest());
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    @Test
    public void voteActivity_success() {
        User user = new User();
        user.setId(1L);

        ActivityVoteRequestDTO voteRequest = new ActivityVoteRequestDTO();
        voteRequest.setVoteType("UP");

        ActivityVoteResponseDTO responseDTO = new ActivityVoteResponseDTO();
        responseDTO.setActivityId(10L);
        responseDTO.setUpvotes(1);
        responseDTO.setDownvotes(0);
        responseDTO.setScore(1);
        responseDTO.setUserVote("UP");

        Mockito.when(userService.validateToken("Bearer token")).thenReturn(user);
        Mockito.when(activityManagementService.voteOnActivity(Mockito.eq(10L), Mockito.eq(1L), Mockito.any(ActivityVoteRequestDTO.class)))
            .thenReturn(responseDTO);

        try {
            mockMvc.perform(put("/activities/10/vote")
                            .header("Authorization", "Bearer token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(new ObjectMapper().writeValueAsString(voteRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.activityId").value(10))
                    .andExpect(jsonPath("$.upvotes").value(1))
                    .andExpect(jsonPath("$.userVote").value("UP"));
        }
        catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    @Test
    public void getTripComments_success() {
        User requester = new User();
        requester.setId(1L);

        User author = new User();
        author.setId(2L);
        author.setUsername("bob");

        Comment comment = new Comment();
        comment.setId(5L);
        comment.setTripId(1L);
        comment.setDestinationId(2L);
        comment.setActivityId(10L);
        comment.setUserId(2L);
        comment.setContent("Great plan");
        comment.setCreatedAt(java.time.LocalDateTime.of(2026, 5, 9, 10, 0));

        Mockito.when(userService.validateToken("Bearer token")).thenReturn(requester);
        Mockito.when(commentService.getCommentsForTrip(1L, 1L)).thenReturn(List.of(comment));
        Mockito.when(userService.getUserById(2L)).thenReturn(author);

        try {
            mockMvc.perform(get("/trips/1/comments")
                            .header("Authorization", "Bearer token")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(5))
                    .andExpect(jsonPath("$[0].username").value("bob"))
                    .andExpect(jsonPath("$[0].content").value("Great plan"));
        }
        catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    @Test
    public void searchActivitiesLegacy_success() {
        User user = new User();
        user.setId(1L);

        ActivitySearchResultDTO result = new ActivitySearchResultDTO();
        result.setPlaceId("legacy-1");
        result.setName("Legacy Search Result");

        Mockito.when(userService.validateToken("Bearer token")).thenReturn(user);
        Mockito.when(activitySearchService.searchActivities(null, null, "museum", "Zurich", 2000))
                .thenReturn(List.of(result));

        try {
            mockMvc.perform(get("/activities/search")
                            .header("Authorization", "Bearer token")
                            .param("query", "museum")
                            .param("location", "Zurich")
                            .param("radius", "2000")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].placeId").value("legacy-1"));
        }
        catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    @Test
    public void getActivityPhoto_success() {
        byte[] imageBytes = new byte[] {1, 2, 3};
        ResponseEntity<byte[]> photoResponse = ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(imageBytes);

        Mockito.when(activitySearchService.fetchPhoto("photo-ref", 400)).thenReturn(photoResponse);

        try {
            mockMvc.perform(get("/activities/photo")
                            .param("photoReference", "photo-ref")
                            .param("maxwidth", "400"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.IMAGE_JPEG))
                    .andExpect(content().bytes(imageBytes));
        }
        catch (Exception exception) {
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