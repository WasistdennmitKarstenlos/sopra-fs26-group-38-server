package ch.uzh.ifi.hase.soprafs26.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;

/**
 * Test class for the UserService integration tests.
 *
 * @see UserService
 */
@WebAppConfiguration
@SpringBootTest
public class UserServiceIntegrationTest {

	@Qualifier("userRepository")
	@Autowired
	private UserRepository userRepository;

	@Autowired
	private UserService userService;

	@BeforeEach
	public void setup() {
		userRepository.deleteAll();
	}

	private User createPersistedUser(String username, String password) {
		User user = new User();
		user.setUsername(username);
		user.setPassword(password);
		user.setBio("Test bio");
		user.setStatus(UserStatus.OFFLINE);
		user.setCreationDate("2026-05-05");
		return userRepository.saveAndFlush(user);
	}

	@Test
	public void getUsers_success() {
		User user1 = createPersistedUser("testuser", "password123");
		User user2 = createPersistedUser("anotheruser", "password456");

		var users = userService.getUsers();

		assertEquals(2, users.size());
		assertTrue(users.stream().anyMatch(user -> user.getId().equals(user1.getId())));
		assertTrue(users.stream().anyMatch(user -> user.getId().equals(user2.getId())));
	}

	@Test
	public void getUserById_validId_returnsUser() {
		User user = createPersistedUser("testuser", "password123");

		User found = userService.getUserById(user.getId());

		assertNotNull(found);
		assertEquals(user.getId(), found.getId());
		assertEquals(user.getUsername(), found.getUsername());
	}

	@Test
	public void getUserById_invalidId_throwsNotFound() {
		assertThrows(ResponseStatusException.class, () -> {
			userService.getUserById(99999L);
		});
	}

	@Test
	public void registerUser_validUser_registersAndLogsIn() {
		User user = new User();
		user.setUsername("newuser");
		user.setPassword("password123");
		user.setBio("New user bio");

		User registered = userService.registerUser(user);

		assertNotNull(registered.getId());
		assertEquals("newuser", registered.getUsername());
		assertEquals(UserStatus.ONLINE, registered.getStatus());
		assertNotNull(registered.getToken());
		assertNotNull(registered.getCreationDate());
	}

	@Test
	public void registerUser_nullUsername_throwsBadRequest() {
		User user = new User();
		user.setPassword("password123");

		assertThrows(ResponseStatusException.class, () -> {
			userService.registerUser(user);
		});
	}

	@Test
	public void registerUser_emptyUsername_throwsBadRequest() {
		User user = new User();
		user.setUsername("   ");
		user.setPassword("password123");

		assertThrows(ResponseStatusException.class, () -> {
			userService.registerUser(user);
		});
	}

	@Test
	public void registerUser_nullPassword_throwsBadRequest() {
		User user = new User();
		user.setUsername("newuser");

		assertThrows(ResponseStatusException.class, () -> {
			userService.registerUser(user);
		});
	}

	@Test
	public void registerUser_emptyPassword_throwsBadRequest() {
		User user = new User();
		user.setUsername("newuser");
		user.setPassword("   ");

		assertThrows(ResponseStatusException.class, () -> {
			userService.registerUser(user);
		});
	}

	@Test
	public void registerUser_duplicateUsername_throwsConflict() {
		createPersistedUser("testuser", "password123");
		User user = new User();
		user.setUsername("testuser");
		user.setPassword("anotherpassword");

		assertThrows(ResponseStatusException.class, () -> {
			userService.registerUser(user);
		});
	}

	@Test
	public void loginUser_validCredentials_logsIn() {
		User user = createPersistedUser("testuser", "password123");

		User loggedIn = userService.loginUser("testuser", "password123");

		assertEquals(user.getId(), loggedIn.getId());
		assertEquals(UserStatus.ONLINE, loggedIn.getStatus());
		assertNotNull(loggedIn.getToken());
	}

	@Test
	public void loginUser_invalidPassword_throwsUnauthorized() {
		createPersistedUser("testuser", "password123");

		assertThrows(ResponseStatusException.class, () -> {
			userService.loginUser("testuser", "wrongpassword");
		});
	}

	@Test
	public void loginUser_nullUsername_throwsBadRequest() {
		assertThrows(ResponseStatusException.class, () -> {
			userService.loginUser(null, "password123");
		});
	}

	@Test
	public void loginUser_emptyUsername_throwsBadRequest() {
		assertThrows(ResponseStatusException.class, () -> {
			userService.loginUser("   ", "password123");
		});
	}

	@Test
	public void loginUser_nullPassword_throwsBadRequest() {
		assertThrows(ResponseStatusException.class, () -> {
			userService.loginUser("testuser", null);
		});
	}

	@Test
	public void loginUser_emptyPassword_throwsBadRequest() {
		assertThrows(ResponseStatusException.class, () -> {
			userService.loginUser("testuser", "   ");
		});
	}

	@Test
	public void loginUser_nonExistentUser_throwsUnauthorized() {
		assertThrows(ResponseStatusException.class, () -> {
			userService.loginUser("nonexistent", "password123");
		});
	}

	@Test
	public void loginUser_alreadyOnline_replacesToken() {
		createPersistedUser("testuser", "password123");
		User firstLogin = userService.loginUser("testuser", "password123");
		User secondLogin = userService.loginUser("testuser", "password123");

		assertNotEquals(firstLogin.getToken(), secondLogin.getToken());
		assertEquals(UserStatus.ONLINE, secondLogin.getStatus());
	}

	@Test
	public void validateToken_rawToken_returnsUser() {
		User user = createPersistedUser("testuser", "password123");
		User loggedIn = userService.loginUser("testuser", "password123");

		User validated = userService.validateToken(loggedIn.getToken());

		assertEquals(user.getId(), validated.getId());
		assertEquals(UserStatus.ONLINE, validated.getStatus());
	}

	@Test
	public void validateToken_bearerToken_returnsUser() {
		User user = createPersistedUser("testuser", "password123");
		User loggedIn = userService.loginUser("testuser", "password123");

		User validated = userService.validateToken("Bearer " + loggedIn.getToken());

		assertEquals(user.getId(), validated.getId());
	}

	@Test
	public void validateToken_nullToken_throwsUnauthorized() {
		assertThrows(ResponseStatusException.class, () -> {
			userService.validateToken(null);
		});
	}

	@Test
	public void validateToken_emptyToken_throwsUnauthorized() {
		assertThrows(ResponseStatusException.class, () -> {
			userService.validateToken("   ");
		});
	}

	@Test
	public void validateToken_invalidToken_throwsUnauthorized() {
		assertThrows(ResponseStatusException.class, () -> {
			userService.validateToken("invalid-token");
		});
	}

	@Test
	public void logoutUser_validRawToken_logsOut() {
		createPersistedUser("testuser", "password123");
		User loggedIn = userService.loginUser("testuser", "password123");

		userService.logoutUser(loggedIn.getToken());

		User storedUser = userRepository.findByUsername("testuser");
		assertEquals(UserStatus.OFFLINE, storedUser.getStatus());
		assertNull(storedUser.getToken());
	}

	@Test
	public void logoutUser_validBearerToken_logsOut() {
		createPersistedUser("testuser", "password123");
		User loggedIn = userService.loginUser("testuser", "password123");

		userService.logoutUser("Bearer " + loggedIn.getToken());

		User storedUser = userRepository.findByUsername("testuser");
		assertEquals(UserStatus.OFFLINE, storedUser.getStatus());
		assertNull(storedUser.getToken());
	}

	@Test
	public void logoutUser_nonExistentToken_throwsNotFound() {
		assertThrows(ResponseStatusException.class, () -> {
			userService.logoutUser("nonexistent");
		});
	}

	@Test
	public void logoutUser_nullToken_throwsBadRequest() {
		assertThrows(ResponseStatusException.class, () -> {
			userService.logoutUser(null);
		});
	}

	@Test
	public void logoutUser_emptyToken_throwsBadRequest() {
		assertThrows(ResponseStatusException.class, () -> {
			userService.logoutUser("   ");
		});
	}
}
