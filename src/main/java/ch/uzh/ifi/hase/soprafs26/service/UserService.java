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

	/**
	 * Creates a new user.
	 */
	public User createUser(User newUser) {
		validateCredentials(newUser.getUsername(), newUser.getPassword());

		newUser.setToken(UUID.randomUUID().toString());
		newUser.setStatus(UserStatus.OFFLINE);
		newUser.setCreationDate(LocalDate.now().toString());
		checkIfUserExists(newUser);

		newUser = userRepository.save(newUser);
		userRepository.flush();

		log.debug("Created Information for User: {}", newUser);
		return newUser;
	}

	/**
	 * Register a new user (self-registration with auto-login).
	 */
	public User registerUser(User newUser) {
		validateCredentials(newUser.getUsername(), newUser.getPassword());

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
	 * Login a user with username and password.
	 */
	public User loginUser(String username, String password) {
		validateCredentials(username, password);

		User user = userRepository.findByUsername(username);
		if (user == null || !password.equals(user.getPassword())) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password!");
		}

		user.setStatus(UserStatus.ONLINE);
		user.setToken(UUID.randomUUID().toString());
		userRepository.save(user);
		userRepository.flush();

		log.debug("User logged in: {}", user);
		return user;
	}

	/**
	 * Logout a user by invalidating their token and setting status to OFFLINE.
	 */
	public void logoutUser(String token) {
		if (token == null || token.trim().isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token cannot be empty!");
		}

		String actualToken = token.startsWith("Bearer ") ? token.substring(7) : token;
		User user = userRepository.findByToken(actualToken);
		if (user == null) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired token!");
		}

		user.setStatus(UserStatus.OFFLINE);
		user.setToken(null);
		userRepository.save(user);
		userRepository.flush();

		log.debug("User logged out: {}", user);
	}

	/**
	 * Validate an Authorization token and return the corresponding user.
	 */
	public User validateToken(String token) {
		if (token == null || token.trim().isEmpty()) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authorization token is required!");
		}

		String actualToken = token.startsWith("Bearer ") ? token.substring(7) : token;
		User user = userRepository.findByToken(actualToken);
		if (user == null) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired token!");
		}

		return user;
	}

	private void validateCredentials(String username, String password) {
		if (username == null || username.trim().isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username cannot be empty!");
		}
		if (password == null || password.trim().isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password cannot be empty!");
		}
	}

	private void checkIfUserExists(User userToBeCreated) {
		User userByUsername = userRepository.findByUsername(userToBeCreated.getUsername());
		if (userByUsername != null) {
			throw new ResponseStatusException(HttpStatus.CONFLICT,
					"The username provided is not unique. Therefore, the user could not be created!");
		}
	}
	
}
