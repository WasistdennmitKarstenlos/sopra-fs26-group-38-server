package ch.uzh.ifi.hase.soprafs26;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "app.cors.allowed-origins=http://localhost:3000,https://sopra-fs26-group-38-client.vercel.app")
class ApplicationCorsTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void corsPreflight_allowsConfiguredOrigin() throws Exception {
		mockMvc.perform(options("/users")
				.header(HttpHeaders.ORIGIN, "http://localhost:3000")
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS,
						"authorization,content-type,access-control-allow-origin"))
				.andExpect(status().isOk())
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:3000"))
				.andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS));
	}

	@Test
	void corsPreflight_rejectsUnconfiguredOrigin() throws Exception {
		mockMvc.perform(options("/users")
				.header(HttpHeaders.ORIGIN, "https://malicious.example")
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
				.andExpect(status().isForbidden())
				.andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
	}

	@Test
	void parseAllowedOrigins_rejectsWildcards() {
		assertThrows(IllegalStateException.class, () -> Application.parseAllowedOrigins("*"));
	}

	@Test
	void parseAllowedOrigins_normalizesTrustedOrigins() {
		assertArrayEquals(
				new String[] { "http://localhost:3000", "https://sopra-fs26-group-38-client.vercel.app" },
				Application.parseAllowedOrigins(
						" http://localhost:3000/ , https://sopra-fs26-group-38-client.vercel.app "));
	}
}
