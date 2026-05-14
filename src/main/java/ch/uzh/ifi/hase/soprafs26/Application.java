package ch.uzh.ifi.hase.soprafs26;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@RestController
@SpringBootApplication
@ConfigurationPropertiesScan
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

	@GetMapping(value = "/", produces = MediaType.TEXT_PLAIN_VALUE)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public String helloWorld() {
		return "The application is running.";
	}

	@Bean
	public FilterRegistrationBean<CorsFilter> corsFilter(
			@Value("${app.cors.allowed-origins}") String allowedOriginsProperty) {
		String[] allowedOrigins = parseAllowedOrigins(allowedOriginsProperty);

		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOrigins(Arrays.asList(allowedOrigins));
		configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
		configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "Cache-Control", "Pragma"));
		configuration.setAllowCredentials(false);
		configuration.setMaxAge(3600L);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);

		FilterRegistrationBean<CorsFilter> registration = new FilterRegistrationBean<>(new CorsFilter(source));
		registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
		return registration;
	}

	static String[] parseAllowedOrigins(String allowedOriginsProperty) {
		String[] allowedOrigins = Arrays.stream(allowedOriginsProperty.split(","))
				.map(String::trim)
				.map(Application::removeTrailingSlashes)
				.filter(origin -> !origin.isEmpty())
				.toArray(String[]::new);

		if (allowedOrigins.length == 0) {
			throw new IllegalStateException("At least one CORS origin must be configured.");
		}

		for (String origin : allowedOrigins) {
			if (origin.contains("*")) {
				throw new IllegalStateException("Wildcard CORS origins are not allowed.");
			}
			validateOrigin(origin);
		}

		return allowedOrigins;
	}

	private static String removeTrailingSlashes(String origin) {
		while (origin.endsWith("/")) {
			origin = origin.substring(0, origin.length() - 1);
		}
		return origin;
	}

	private static void validateOrigin(String origin) {
		URI uri = URI.create(origin);
		if (!("http".equals(uri.getScheme()) || "https".equals(uri.getScheme()))
				|| uri.getHost() == null
				|| uri.getUserInfo() != null
				|| uri.getPath() != null && !uri.getPath().isEmpty()
				|| uri.getQuery() != null
				|| uri.getFragment() != null) {
			throw new IllegalStateException("Invalid CORS origin: " + origin);
		}
	}
}
