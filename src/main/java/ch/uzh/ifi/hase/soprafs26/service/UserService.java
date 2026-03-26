package ch.uzh.ifi.hase.soprafs26.service;

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

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

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

	public User createUser(User newUser) {
		newUser.setToken(UUID.randomUUID().toString());
		newUser.setStatus(UserStatus.OFFLINE);
		newUser.setCreationDate(LocalDate.now().toString());
		checkIfUserExists(newUser);
		// saves the given entity but data is only persisted in the database once
		// flush() is called
		newUser = userRepository.save(newUser);
		userRepository.flush();

		log.debug("Created Information for User: {}", newUser);
		return newUser;
	}

	/**
	 * Login a user with username and password.
	 * Returns the user with a fresh token and ONLINE status on success.
	 * Throws 400 if fields are empty, 401 if credentials are wrong.
	 *
	 * @param username
	 * @param password
	 * @return the authenticated User
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
		if (!password.equals(user.getPassword())) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password!");
		}

		// Set user status to ONLINE and generate a fresh token
		user.setStatus(UserStatus.ONLINE);
		user.setToken(UUID.randomUUID().toString());
		userRepository.save(user);
		userRepository.flush();

		log.debug("User logged in: {}", user);
		return user;
	}

	/**
	 * Logout a user by invalidating their token and setting status to OFFLINE.
	 * Throws 401 if the token is invalid or not found.
	 *
	 * @param token the raw Authorization header value (with or without "Bearer " prefix)
	 */
	public void logoutUser(String token) {
		if (token == null || token.trim().isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token cannot be empty!");
		}
		// Strip "Bearer " prefix if present
		String actualToken = token.startsWith("Bearer ") ? token.substring(7) : token;

		User user = userRepository.findByToken(actualToken);
		if (user == null) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired token!");
		}

		// Invalidate session
		user.setStatus(UserStatus.OFFLINE);
		user.setToken(null);
		userRepository.save(user);
		userRepository.flush();

		log.debug("User logged out: {}", user);
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
	 * username and (if provided) the name defined in the User entity.
	 * The method will do nothing if the input is unique and throw an error otherwise.
	 *
	 * @param userToBeCreated
	 * @throws org.springframework.web.server.ResponseStatusException
	 * @see User
	 */
	private void checkIfUserExists(User userToBeCreated) {
		User userByUsername = userRepository.findByUsername(userToBeCreated.getUsername());
		// Only check name uniqueness when a name is explicitly provided
		User userByName = userToBeCreated.getName() != null
				? userRepository.findByName(userToBeCreated.getName())
				: null;

		String baseErrorMessage = "The %s provided %s not unique. Therefore, the user could not be created!";
		if (userByUsername != null && userByName != null) {
			throw new ResponseStatusException(HttpStatus.CONFLICT,
					String.format(baseErrorMessage, "username and the name", "are"));
		} else if (userByUsername != null) {
			throw new ResponseStatusException(HttpStatus.CONFLICT,
					String.format(baseErrorMessage, "username", "is"));
		} else if (userByName != null) {
			throw new ResponseStatusException(HttpStatus.CONFLICT,
					String.format(baseErrorMessage, "name", "is"));
		}
	}
}
