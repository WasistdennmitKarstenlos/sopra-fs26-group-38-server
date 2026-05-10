package ch.uzh.ifi.hase.soprafs26.service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;

/**
 * User Service
 * This class is the "worker" and responsible for all functionality related to
 * the user
 * (e.g., it creates, modifies, deletes, finds). The result will be passed back
 * to the caller.
 */
@Service
@Transactional
public class UserService {

	private final Logger log = LoggerFactory.getLogger(UserService.class);

	private final UserRepository userRepository;

	public UserService(@Qualifier("userRepository") UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	public List<User> getUsers() {
		return this.userRepository.findAll();
	}

	public User getUserById(Long userId) {
		return userRepository.findById(userId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
	}

	/**
	 * Update the username for an authenticated user.
	 *
	 * @param userId the user id to update
	 * @param newUsername the new username value
	 * @return the updated user
	 */
	public User updateUsername(Long userId, String newUsername) {
		if (newUsername == null || newUsername.trim().isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username cannot be empty!");
		}
		String sanitizedUsername = newUsername.trim();
		if (sanitizedUsername.length() < 3) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username must be at least 3 characters!");
		}
		if (sanitizedUsername.length() > 50) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username must be at most 50 characters!");
		}

		User user = getUserById(userId);
		User existingUser = userRepository.findByUsername(sanitizedUsername);
		if (existingUser != null && !existingUser.getId().equals(userId)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "The username provided is not unique. Therefore, the user could not be updated!");
		}

		user.setUsername(sanitizedUsername);
		userRepository.save(user);
		userRepository.flush();

		log.info("Updated username for user {} to {}", userId, sanitizedUsername);
		return user;
	}

	/**
	 * Update the password for an authenticated user and rotate their token.
	 *
	 * @param userId the user id to update
	 * @param currentPassword the existing password for verification
	 * @param newPassword the new password value
	 * @return the updated user with a rotated token
	 */
	public User updatePassword(Long userId, String currentPassword, String newPassword) {
		if (currentPassword == null || currentPassword.trim().isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password cannot be empty!");
		}
		if (newPassword == null || newPassword.trim().isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "New password cannot be empty!");
		}
		if (newPassword.trim().length() < 8) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "New password must be at least 8 characters!");
		}

		User user = getUserById(userId);
		if (!user.getPassword().equals(currentPassword)) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Current password is incorrect!");
		}

		user.setPassword(newPassword);
		user.setToken(UUID.randomUUID().toString());
		user.setStatus(UserStatus.ONLINE);
		userRepository.save(user);
		userRepository.flush();

		log.info("Updated password for user {} and rotated token", userId);
		return user;
	}

	/**
	 * Register a new user (self-registration with auto-login)
	*/
	public User registerUser(User newUser) {
		// Validate that username and password are not empty
		if (newUser.getUsername() == null || newUser.getUsername().trim().isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username cannot be empty!");
		}
		if (newUser.getPassword() == null || newUser.getPassword().trim().isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password cannot be empty!");
		}
		
		newUser.setToken(UUID.randomUUID().toString()); 
		newUser.setStatus(UserStatus.ONLINE);
		newUser.setCreationDate(LocalDate.now().toString()); 
		checkIfUserExists(newUser);
		newUser = userRepository.save(newUser);
		userRepository.flush();

		log.debug("Registered and logged in User: {}", newUser);
		return newUser;
	}

	/**
	 * Login a user with username and password
	 * @param username
	 * @param password
	 * @return User
	 */
	public User loginUser(String username, String password) {
		// Validate that username and password are not empty
		if (username == null || username.trim().isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username cannot be empty!");
		}
		if (password == null || password.trim().isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password cannot be empty!");
		}

		User user = userRepository.findByUsername(username);
		if (user == null) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password!");
		}
		
		// Check if password matches
		if (!user.getPassword().equals(password)) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password!");
		}

		// Set user status to ONLINE
		user.setStatus(UserStatus.ONLINE);
        user.setToken(UUID.randomUUID().toString()); // Generate new token on login
		userRepository.save(user);
		userRepository.flush();

		log.debug("User logged in: {}", user);
		return user;
	}
    
	/**
	 * Validate an Authorization token and return the corresponding user.
	 * Throws 401 if the token is missing, invalid, or expired.
	 *
	 * @param token the raw Authorization header value (with or without "Bearer " prefix)
	 * @return the authenticated User
	 */
	public User validateToken(String token) {
		if (token == null || token.trim().isEmpty()) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authorization token is required!");
		}
		// Strip "Bearer " prefix if present
		String actualToken = token.startsWith("Bearer ") ? token.substring(7) : token;

		User user = userRepository.findByToken(actualToken);
		if (user == null) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired token!");
		}

		return user;
	}

    /**
	 * This is a helper method that will check the uniqueness criteria of the
	 * username and the name
	 * defined in the User entity. The method will do nothing if the input is unique
	 * and throw an error otherwise.
	 *
	 * @param userToBeCreated
	 * @throws org.springframework.web.server.ResponseStatusException
	 * @see User
	 */
	private void checkIfUserExists(User userToBeCreated) {
		User userByUsername = userRepository.findByUsername(userToBeCreated.getUsername());

		String baseErrorMessage = "The %s provided %s not unique. Therefore, the user could not be created!";
		if (userByUsername != null) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, String.format(baseErrorMessage, "username", "is"));
		}
	}

	/**
	 * Logout a user
	 * @param token
	 */
	public void logoutUser(String token) {
		if (token == null || token.trim().isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token cannot be empty!");
		}
		// Remove "Bearer " prefix if present
		String actualToken = token.startsWith("Bearer ") ? token.substring(7) : token;

		User user = userRepository.findByToken(actualToken);
		if (user == null) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found!");
		}

		// Set user status to OFFLINE
		user.setStatus(UserStatus.OFFLINE);
        user.setToken(null); // Clear token on logout
		userRepository.save(user);
		userRepository.flush();

		log.debug("User logged out: {}", user);
	}
}
