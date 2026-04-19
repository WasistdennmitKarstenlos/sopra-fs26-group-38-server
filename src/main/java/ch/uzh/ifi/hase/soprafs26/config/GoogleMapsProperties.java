package ch.uzh.ifi.hase.soprafs26.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "google.maps")
public record GoogleMapsProperties(
        String baseUrl,
        String apiKey
) {
}

