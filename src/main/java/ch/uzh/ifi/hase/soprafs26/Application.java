package ch.uzh.ifi.hase.soprafs26;

import java.net.URI;
import java.util.Arrays;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

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
	public WebMvcConfigurer corsConfigurer(@Value("${app.cors.allowed-origins}") String allowedOriginsProperty) {
		String[] allowedOrigins = parseAllowedOrigins(allowedOriginsProperty);

		return new WebMvcConfigurer() {
			@Override
			public void addCorsMappings(CorsRegistry registry) {
				registry.addMapping("/**")
						.allowedOrigins(allowedOrigins)
						.allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
						.allowedHeaders(
								"Authorization",
								"Content-Type",
								"Accept",
								"Cache-Control",
								"Pragma",
								"Access-Control-Allow-Origin")
						.allowCredentials(false)
						.maxAge(3600);
			}
		};
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
