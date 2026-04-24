package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.config.GoogleMapsProperties;
import ch.uzh.ifi.hase.soprafs26.rest.dto.ActivitySearchResultDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ActivitySearchServiceTest {

    @Mock
    private RestTemplate restTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private ActivitySearchService activitySearchService;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        activitySearchService = new ActivitySearchService(
                restTemplate,
                objectMapper,
                new GoogleMapsProperties(
                        "https://maps.googleapis.com/maps/api/place/textsearch/json",
                        "test-key"));
    }

    @Test
    public void searchActivities_validResponse_success() throws Exception {
        String googleResponse = """
                {
                  "status": "OK",
                  "results": [
                    {
                      "place_id": "place-1",
                      "name": "City Museum",
                      "formatted_address": "Main Street 1",
                      "rating": 4.6,
                      "photos": [
                        {"photo_reference": "photo-ref-1"}
                      ],
                      "geometry": {
                        "location": {
                          "lat": 47.3769,
                          "lng": 8.5417
                        }
                      }
                    }
                  ]
                }
                """;

        Mockito.when(restTemplate.getForEntity(Mockito.anyString(), Mockito.eq(String.class)))
                .thenReturn(new ResponseEntity<>(googleResponse, HttpStatus.OK));

        List<ActivitySearchResultDTO> results = activitySearchService.searchActivities(1L, 2L, "museum", "Zurich", 2000);

        assertEquals(1, results.size());
        assertEquals("place-1", results.get(0).getPlaceId());
        assertEquals("City Museum", results.get(0).getName());
        assertEquals("Main Street 1", results.get(0).getAddress());
        assertEquals(4.6, results.get(0).getRating());
        assertEquals("/activities/photo?photoReference=photo-ref-1&maxwidth=800", results.get(0).getPhotoUrl());
        assertEquals(47.3769, results.get(0).getLatitude());
        assertEquals(8.5417, results.get(0).getLongitude());
    }

    @Test
    public void searchActivities_zeroResults_returnsEmptyList() {
        String googleResponse = """
                {
                  "status": "ZERO_RESULTS",
                  "results": []
                }
                """;

        Mockito.when(restTemplate.getForEntity(Mockito.anyString(), Mockito.eq(String.class)))
                .thenReturn(new ResponseEntity<>(googleResponse, HttpStatus.OK));

        List<ActivitySearchResultDTO> results = activitySearchService.searchActivities(1L, 2L, "museum", "Zurich", 2000);

        assertEquals(0, results.size());
    }

    @Test
    public void searchActivities_blankQuery_throwsException() {
        assertThrows(ResponseStatusException.class,
          () -> activitySearchService.searchActivities(1L, 2L, " ", "Zurich", 2000));
    }
}