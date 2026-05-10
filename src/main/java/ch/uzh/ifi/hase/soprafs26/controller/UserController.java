package ch.uzh.ifi.hase.soprafs26.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UserGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UserLoginDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UserRegisterDTO;
import ch.uzh.ifi.hase.soprafs26.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs26.service.UserService;

import java.util.List;

// Testing sonarcloud Server
/**
 * User Controller
 * This class is responsible for handling all REST request that are related to
 * the user.
 * The controller will receive the request and delegate the execution to the
 * UserService and finally return the result.
 */
@RestController
public class UserController {

	private final UserService userService;

	UserController(UserService userService) {
		this.userService = userService;
	}

	/**
	 * POST /auth/login
	 * Authenticates a user with username and password.
	 * Returns 200 with user data (including token) on success.
	 * Returns 401 if credentials are invalid.
	 */
	@PostMapping("/auth/login")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public UserGetDTO loginUser(@RequestBody UserLoginDTO userLoginDTO) {
		User loggedInUser = userService.loginUser(
				userLoginDTO.getUsername(),
				userLoginDTO.getPassword());
		return DTOMapper.INSTANCE.convertEntityToUserGetDTO(loggedInUser);
	}

	/**
	 * POST /auth/logout
	 * Ends the current session for the authenticated user.
	 * Requires a valid Authorization header (Bearer token).
	 * Returns 204 No Content on success.
	 */
	@PostMapping("/auth/logout")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void logoutUser(
			@RequestHeader(value = "Authorization", required = false) String token) {
		userService.logoutUser(token);
	}

		/**
		 * GET /users
		 * Returns all registered users for authenticated clients.
		 */
		@GetMapping("/users")
		@ResponseStatus(HttpStatus.OK)
		@ResponseBody
		public List<UserGetDTO> getUsers(
				@RequestHeader(value = "Authorization", required = false) String token) {
			userService.validateToken(token);
			return userService.getUsers().stream()
					.map(DTOMapper.INSTANCE::convertEntityToUserGetDTO)
					.toList();
		}

		/**
		 * GET /users/{userId}
		 * Returns a single user profile for authenticated clients.
		 */
		@GetMapping("/users/{userId}")
		@ResponseStatus(HttpStatus.OK)
		@ResponseBody
		public UserGetDTO getUserById(
				@PathVariable Long userId,
				@RequestHeader(value = "Authorization", required = false) String token) {
			userService.validateToken(token);
			User user = userService.getUserById(userId);
			return DTOMapper.INSTANCE.convertEntityToUserGetDTO(user);
		}
    
	// Frontend register page send info here 
	@PostMapping("/users/register")
	@ResponseStatus(HttpStatus.CREATED)
	@ResponseBody
	public UserGetDTO registerUser(@RequestBody UserRegisterDTO userRegisterDTO) {
		// convert API user to internal representation
		User userInput = DTOMapper.INSTANCE.convertUserRegisterDTOtoEntity(userRegisterDTO);

		// register user (self-registration with auto-login)
		User registeredUser = userService.registerUser(userInput);
		// convert internal representation of user back to API
		return DTOMapper.INSTANCE.convertEntityToUserGetDTO(registeredUser);
	}

}
