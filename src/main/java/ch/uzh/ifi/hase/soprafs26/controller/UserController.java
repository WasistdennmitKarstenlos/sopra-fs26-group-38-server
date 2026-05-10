package ch.uzh.ifi.hase.soprafs26.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UserUpdatePasswordDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UserGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UserLoginDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UserRegisterDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UserUpdateUsernameDTO;
import ch.uzh.ifi.hase.soprafs26.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs26.service.UserService;

import java.util.ArrayList;
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

	/**
	 * GET /users/{id}
	 * Fetch a user by their ID.
	 * Returns 200 with user data if found, 404 otherwise.
	 */
	@GetMapping("/users/{id}")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public UserGetDTO getUserById(@PathVariable("id") Long id) {
		User user = userService.getUserById(id);
		return DTOMapper.INSTANCE.convertEntityToUserGetDTO(user);
	}

	/**
	 * PUT /users/me/username
	 * Updates the authenticated user's username.
	 */
	@PutMapping("/users/me/username")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public UserGetDTO updateUsername(
			@RequestHeader(value = "Authorization", required = false) String token,
			@RequestBody UserUpdateUsernameDTO userUpdateUsernameDTO) {
		User authenticatedUser = userService.validateToken(token);
		User updatedUser = userService.updateUsername(
				authenticatedUser.getId(),
				userUpdateUsernameDTO.getNewUsername());
		return DTOMapper.INSTANCE.convertEntityToUserGetDTO(updatedUser);
	}

	/**
	 * PUT /users/me/password
	 * Updates the authenticated user's password and rotates the token.
	 */
	@PutMapping("/users/me/password")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public UserGetDTO updatePassword(
			@RequestHeader(value = "Authorization", required = false) String token,
			@RequestBody UserUpdatePasswordDTO userUpdatePasswordDTO) {
		User authenticatedUser = userService.validateToken(token);
		User updatedUser = userService.updatePassword(
				authenticatedUser.getId(),
				userUpdatePasswordDTO.getCurrentPassword(),
				userUpdatePasswordDTO.getNewPassword());
		return DTOMapper.INSTANCE.convertEntityToUserGetDTO(updatedUser);
	}

}
