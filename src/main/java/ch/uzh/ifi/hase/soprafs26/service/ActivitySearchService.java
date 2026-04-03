package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.rest.dto.ActivitySearchResultDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ActivitySearchService {

    private final Logger log = LoggerFactory.getLogger(ActivitySearchService.class);
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final String apiKey;

    public ActivitySearchService(RestTemplate restTemplate,
                                 ObjectMapper objectMapper,
                                 @Value("${google.maps.base-url:https://maps.googleapis.com/maps/api/place/textsearch/json}") String baseUrl,
                                 @Value("${google.maps.api-key:}") String apiKey) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
    }

    public List<ActivitySearchResultDTO> searchActivities(Long tripId,
                                                          Long destinationId,
                                                          String query,
                                                          String location,
                                                          Integer radius) {
        if (query == null || query.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Query cannot be empty");
        }

        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Google Maps API key is not configured");
        }

        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(baseUrl)
                .queryParam("query", query.trim())
                .queryParam("key", apiKey.trim());

        if (location != null && !location.trim().isEmpty()) {
            uriBuilder.queryParam("location", location.trim());
        }

        if (radius != null && radius > 0) {
            uriBuilder.queryParam("radius", radius);
        }

        String requestUrl = uriBuilder.toUriString();
        log.debug("Searching activities for trip {} destination {} via Google Places: {}", tripId, destinationId, requestUrl);

        ResponseEntity<String> response = restTemplate.getForEntity(requestUrl, String.class);
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Activity search provider returned an invalid response");
        }

        try {
            GooglePlacesResponse root = objectMapper.readValue(response.getBody(), GooglePlacesResponse.class);
            String status = root.getStatus() != null ? root.getStatus() : "";

            if ("ZERO_RESULTS".equals(status)) {
                return List.of();
            }

            if (!"OK".equals(status)) {
                String errorMessage = root.getErrorMessage() != null ? root.getErrorMessage() : "Unknown Google Places error";
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, errorMessage);
            }

            List<ActivitySearchResultDTO> results = new ArrayList<>();
            if (root.getResults() == null) {
                return results;
            }

            for (GooglePlacesResult resultNode : root.getResults()) {
                results.add(mapResult(resultNode));
            }
            return results;
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to parse activity search response", ex);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Failed to parse activity search response", ex);
        }
    }

    private ActivitySearchResultDTO mapResult(GooglePlacesResult resultNode) {
        ActivitySearchResultDTO dto = new ActivitySearchResultDTO();
        dto.setPlaceId(resultNode.getPlaceId());
        dto.setName(resultNode.getName());
        dto.setAddress(resultNode.getFormattedAddress());

        if (resultNode.getRating() != null) {
            dto.setRating(resultNode.getRating());
        }

        dto.setPhotoUrl(buildPhotoUrl(resultNode));

        if (resultNode.getGeometry() != null && resultNode.getGeometry().getLocation() != null) {
            dto.setLatitude(resultNode.getGeometry().getLocation().getLat());
            dto.setLongitude(resultNode.getGeometry().getLocation().getLng());
        }

        return dto;
    }

    public static class GooglePlacesResponse {
        @JsonProperty("status")
        private String status;
        @JsonProperty("error_message")
        private String errorMessage;
        @JsonProperty("results")
        private List<GooglePlacesResult> results;

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }

        public List<GooglePlacesResult> getResults() {
            return results;
        }

        public void setResults(List<GooglePlacesResult> results) {
            this.results = results;
        }
    }

    public static class GooglePlacesResult {
        @JsonProperty("place_id")
        private String placeId;
        @JsonProperty("name")
        private String name;
        @JsonProperty("formatted_address")
        private String formattedAddress;
        @JsonProperty("rating")
        private Double rating;
        @JsonProperty("photos")
        private List<GooglePhoto> photos;
        @JsonProperty("geometry")
        private GoogleGeometry geometry;

        public String getPlaceId() {
            return placeId;
        }

        public void setPlaceId(String placeId) {
            this.placeId = placeId;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getFormattedAddress() {
            return formattedAddress;
        }

        public void setFormattedAddress(String formattedAddress) {
            this.formattedAddress = formattedAddress;
        }

        public Double getRating() {
            return rating;
        }

        public void setRating(Double rating) {
            this.rating = rating;
        }

        public List<GooglePhoto> getPhotos() {
            return photos;
        }

        public void setPhotos(List<GooglePhoto> photos) {
            this.photos = photos;
        }

        public GoogleGeometry getGeometry() {
            return geometry;
        }

        public void setGeometry(GoogleGeometry geometry) {
            this.geometry = geometry;
        }
    }

    public static class GooglePhoto {
        @JsonProperty("photo_reference")
        private String photoReference;

        public String getPhotoReference() {
            return photoReference;
        }

        public void setPhotoReference(String photoReference) {
            this.photoReference = photoReference;
        }
    }

    public static class GoogleGeometry {
        @JsonProperty("location")
        private GoogleLocation location;

        public GoogleLocation getLocation() {
            return location;
        }

        public void setLocation(GoogleLocation location) {
            this.location = location;
        }
    }

    public static class GoogleLocation {
        @JsonProperty("lat")
        private Double lat;
        @JsonProperty("lng")
        private Double lng;

        public Double getLat() {
            return lat;
        }

        public void setLat(Double lat) {
            this.lat = lat;
        }

        public Double getLng() {
            return lng;
        }

        public void setLng(Double lng) {
            this.lng = lng;
        }
    }

    private String buildPhotoUrl(GooglePlacesResult resultNode) {
        if (resultNode.getPhotos() == null || resultNode.getPhotos().isEmpty() || resultNode.getPhotos().get(0).getPhotoReference() == null) {
            return null;
        }

        return UriComponentsBuilder.fromUriString("https://maps.googleapis.com/maps/api/place/photo")
                .queryParam("maxwidth", 800)
                .queryParam("photo_reference", resultNode.getPhotos().get(0).getPhotoReference())
                .queryParam("key", apiKey.trim())
                .toUriString();
    }
}