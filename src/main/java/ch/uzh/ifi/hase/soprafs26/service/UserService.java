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
    
    /**
	*  Creates a new user
	*/
	public User createUser(User newUser) {
		// Validate that username and password are not empty
		if (newUser.getUsername() == null || newUser.getUsername().trim().isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username cannot be empty!");
		}
		if (newUser.getPassword() == null || newUser.getPassword().trim().isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password cannot be empty!");
		}
		
		newUser.setToken(UUID.randomUUID().toString());
		newUser.setStatus(UserStatus.OFFLINE); 
		newUser.setCreationDate(LocalDate.now().toString());
		checkIfUserExists(newUser);

		// saves the given entity but data is only persisted in the database once flush() is called
		newUser = userRepository.save(newUser);
		userRepository.flush();

		log.debug("Created Information for User: {}", newUser);
		return newUser;
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

	
	/*
	public User createUser(User newUser) {
		newUser.setToken(UUID.randomUUID().toString());
		newUser.setStatus(UserStatus.OFFLINE);
		checkIfUserExists(newUser);
		// saves the given entity but data is only persisted in the database once
		// flush() is called
		newUser = userRepository.save(newUser);
		userRepository.flush();

		log.debug("Created Information for User: {}", newUser);
		return newUser;
	}
	*/
    
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
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format(baseErrorMessage, "username", "is"));
		} 
	}
	
}
