package ch.uzh.ifi.hase.soprafs26.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;

import java.util.Optional;

public class UserServiceTest {

	@Mock
	private UserRepository userRepository;

	@InjectMocks
	private UserService userService;

	private User testUser;

	@BeforeEach
	public void setup() {
		MockitoAnnotations.openMocks(this);

		// given
		testUser = new User();
		testUser.setId(1L);
		testUser.setUsername("testUsername");
		testUser.setPassword("testPassword");
		testUser.setToken("oldToken");

		// when -> any object is being save in the userRepository -> return the dummy
		// testUser
		Mockito.when(userRepository.save(Mockito.any())).thenReturn(testUser);
	}

	@Test
	public void updateUsername_successful_updatesUsername() {
		Mockito.when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
		Mockito.when(userRepository.findByUsername("newUsername")).thenReturn(null);

		User updated = userService.updateUsername(1L, "newUsername");

		assertEquals("newUsername", updated.getUsername());
	}

	@Test
	public void updateUsername_duplicateUsername_throwsConflict() {
		User otherUser = new User();
		otherUser.setId(2L);
		otherUser.setUsername("existingUsername");

		Mockito.when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
		Mockito.when(userRepository.findByUsername("existingUsername")).thenReturn(otherUser);

		ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
			userService.updateUsername(1L, "existingUsername")
		);

		assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
	}

	@Test
	public void updatePassword_successful_updatesPasswordAndRotatesToken() {
		Mockito.when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
		String originalToken = testUser.getToken();

		User updated = userService.updatePassword(1L, "testPassword", "newPassword123");

		assertEquals("newPassword123", updated.getPassword());
		assertNotEquals(originalToken, updated.getToken());
	}

	@Test
	public void updatePassword_wrongCurrentPassword_throwsUnauthorized() {
		Mockito.when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

		ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
			userService.updatePassword(1L, "wrongPassword", "newPassword123")
		);

		assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
	}

}
