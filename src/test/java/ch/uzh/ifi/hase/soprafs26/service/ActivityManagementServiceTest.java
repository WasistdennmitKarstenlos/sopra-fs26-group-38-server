package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.entity.Activity;
import ch.uzh.ifi.hase.soprafs26.repository.ActivityRepository;
import ch.uzh.ifi.hase.soprafs26.rest.dto.ActivityPostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.ActivitySearchResultDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ActivityManagementServiceTest {

    @Mock
    private ActivityRepository activityRepository;

    @Mock
    private TripService tripService;

    @InjectMocks
    private ActivityManagementService activityManagementService;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void addActivity_validInput_success() {
        ActivityPostDTO postDTO = new ActivityPostDTO();
        postDTO.setPlaceId("place-1");
        postDTO.setName("City Museum");
        postDTO.setAddress("Main Street 1");
        postDTO.setRating(4.5);
        postDTO.setPhotoUrl("https://example.com/photo.jpg");

        Activity saved = new Activity();
        saved.setId(1L);
        saved.setTripId(1L);
        saved.setDestinationId(2L);
        saved.setPlaceId("place-1");
        saved.setName("City Museum");
        saved.setAddress("Main Street 1");
        saved.setRating(4.5);
        saved.setPhotoUrl("https://example.com/photo.jpg");

        Mockito.when(activityRepository.findByTripIdAndDestinationIdAndPlaceId(1L, 2L, "place-1"))
                .thenReturn(Optional.empty());
        Mockito.when(activityRepository.save(Mockito.any(Activity.class))).thenReturn(saved);

        ActivitySearchResultDTO result = activityManagementService.addActivity(1L, 2L, postDTO);

        assertEquals("place-1", result.getPlaceId());
        assertEquals("City Museum", result.getName());
        assertEquals("https://example.com/photo.jpg", result.getPhotoUrl());
    }

    @Test
    public void addActivity_duplicate_throwsConflict() {
        ActivityPostDTO postDTO = new ActivityPostDTO();
        postDTO.setPlaceId("place-1");
        postDTO.setName("City Museum");

        Mockito.when(activityRepository.findByTripIdAndDestinationIdAndPlaceId(1L, 2L, "place-1"))
                .thenReturn(Optional.of(new Activity()));

        assertThrows(ResponseStatusException.class, () -> activityManagementService.addActivity(1L, 2L, postDTO));
    }

    @Test
    public void getSelectedActivities_success() {
        Activity activity = new Activity();
        activity.setTripId(1L);
        activity.setDestinationId(2L);
        activity.setPlaceId("place-1");
        activity.setName("City Museum");

        Mockito.when(activityRepository.findByTripIdAndDestinationIdOrderByIdDesc(1L, 2L))
                .thenReturn(List.of(activity));

        List<ActivitySearchResultDTO> result = activityManagementService.getSelectedActivities(1L, 2L);

        assertEquals(1, result.size());
        assertEquals("place-1", result.get(0).getPlaceId());
        assertEquals("City Museum", result.get(0).getName());
    }
}