package ch.uzh.ifi.hase.soprafs26.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import org.junit.jupiter.api.Test;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.web.server.ResponseStatusException;
import java.util.Collections;

import ch.uzh.ifi.hase.soprafs26.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.service.UserService;

/**
 * UserControllerTest
 * This is a WebMvcTest which allows to test the UserController i.e. GET/POST
 * request without actually sending them over the network.
 * This tests if the UserController works.
 */
@WebMvcTest(UserController.class)
public class UserControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private UserService userService;

	@Test
	public void getUsers_successful() throws Exception {
		User user = new User();
		user.setId(1L);
		user.setUsername("testUsername");
		user.setBio("Test bio");
		user.setStatus(UserStatus.ONLINE);

		given(userService.validateToken("Bearer testToken")).willReturn(user);
		given(userService.getUsers()).willReturn(Collections.singletonList(user));

		mockMvc.perform(get("/users").header("Authorization", "Bearer testToken"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].id", is(user.getId().intValue())))
				.andExpect(jsonPath("$[0].username", is(user.getUsername())))
				.andExpect(jsonPath("$[0].bio", is(user.getBio())))
				.andExpect(jsonPath("$[0].status", is(user.getStatus().toString())));
	}

	@Test
	public void getUserById_successful() throws Exception {
		User user = new User();
		user.setId(1L);
		user.setUsername("testUsername");
		user.setBio("Test bio");
		user.setStatus(UserStatus.ONLINE);

		given(userService.validateToken("Bearer testToken")).willReturn(user);
		given(userService.getUserById(1L)).willReturn(user);

		mockMvc.perform(get("/users/1").header("Authorization", "Bearer testToken"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id", is(user.getId().intValue())))
				.andExpect(jsonPath("$.username", is(user.getUsername())))
				.andExpect(jsonPath("$.bio", is(user.getBio())))
				.andExpect(jsonPath("$.status", is(user.getStatus().toString())));
	}

	@Test
	public void loginUser_successful() throws Exception {
		// given
		User user = new User();
		user.setId(1L);
		user.setUsername("testUsername");
		user.setToken("testToken");
		user.setStatus(UserStatus.ONLINE);

		given(userService.loginUser("testUsername", "testPassword")).willReturn(user);

		// when
		MockHttpServletRequestBuilder postRequest = post("/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"username\":\"testUsername\",\"password\":\"testPassword\"}");

		// then
		mockMvc.perform(postRequest)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id", is(user.getId().intValue())))
				.andExpect(jsonPath("$.username", is(user.getUsername())))
				.andExpect(jsonPath("$.token", is(user.getToken())))
				.andExpect(jsonPath("$.status", is(user.getStatus().toString())));
	}

	@Test
	public void loginUser_invalidCredentials_returnsUnauthorized() throws Exception {
		// given
		ResponseStatusException unauthorizedException = new ResponseStatusException(
			org.springframework.http.HttpStatus.UNAUTHORIZED, "Invalid username or password!");
		given(userService.loginUser("wrongUsername", "wrongPassword"))
			.willThrow(unauthorizedException);

		// when
		MockHttpServletRequestBuilder postRequest = post("/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"username\":\"wrongUsername\",\"password\":\"wrongPassword\"}");

		// then
		mockMvc.perform(postRequest)
				.andExpect(status().isUnauthorized());
	}

	@Test
	public void loginUser_emptyUsername_returnsBadRequest() throws Exception {
		// given
		ResponseStatusException badRequestException = new ResponseStatusException(
			org.springframework.http.HttpStatus.BAD_REQUEST, "Username cannot be empty!");
		given(userService.loginUser("", "testPassword"))
			.willThrow(badRequestException);

		// when
		MockHttpServletRequestBuilder postRequest = post("/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"username\":\"\",\"password\":\"testPassword\"}");

		// then
		mockMvc.perform(postRequest)
				.andExpect(status().isBadRequest());
	}

	@Test
	public void loginUser_emptyPassword_returnsBadRequest() throws Exception {
		// given
		ResponseStatusException badRequestException = new ResponseStatusException(
			org.springframework.http.HttpStatus.BAD_REQUEST, "Password cannot be empty!");
		given(userService.loginUser("testUsername", ""))
			.willThrow(badRequestException);

		// when
		MockHttpServletRequestBuilder postRequest = post("/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"username\":\"testUsername\",\"password\":\"\"}");

		// then
		mockMvc.perform(postRequest)
				.andExpect(status().isBadRequest());
	}

	@Test
	public void logoutUser_successful() throws Exception {
		// when
		MockHttpServletRequestBuilder postRequest = post("/auth/logout")
				.header("Authorization", "Bearer testToken");

		// then
		mockMvc.perform(postRequest)
				.andExpect(status().isNoContent());
	}

	@Test
	public void logoutUser_invalidToken_returnsNotFound() throws Exception {
		// given
		ResponseStatusException notFoundException = new ResponseStatusException(
			org.springframework.http.HttpStatus.NOT_FOUND, "User not found!");
		doThrow(notFoundException).when(userService).logoutUser("Bearer invalidToken");

		// when
		MockHttpServletRequestBuilder postRequest = post("/auth/logout")
				.header("Authorization", "Bearer invalidToken");

		// then
		mockMvc.perform(postRequest)
				.andExpect(status().isNotFound());
	}

	@Test
	public void logoutUser_emptyToken_returnsBadRequest() throws Exception {
		// given
		ResponseStatusException badRequestException = new ResponseStatusException(
			org.springframework.http.HttpStatus.BAD_REQUEST, "Token cannot be empty!");
		doThrow(badRequestException).when(userService).logoutUser("");

		// when
		MockHttpServletRequestBuilder postRequest = post("/auth/logout")
				.header("Authorization", "");

		// then
		mockMvc.perform(postRequest)
				.andExpect(status().isBadRequest());
	}

	@Test
	public void getUserById_successful() throws Exception {
		User user = new User();
		user.setId(1L);
		user.setUsername("testUsername");
		user.setStatus(UserStatus.OFFLINE);

		given(userService.getUserById(1L)).willReturn(user);

		mockMvc.perform(get("/users/1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id", is(user.getId().intValue())))
				.andExpect(jsonPath("$.username", is(user.getUsername())));
	}

	@Test
	public void updatePassword_successful() throws Exception {
		User authenticatedUser = new User();
		authenticatedUser.setId(1L);

		User updatedUser = new User();
		updatedUser.setId(1L);
		updatedUser.setUsername("testUsername");
		updatedUser.setToken("newToken");
		updatedUser.setStatus(UserStatus.ONLINE);

		given(userService.validateToken("Bearer testToken")).willReturn(authenticatedUser);
		given(userService.updatePassword(1L, "oldPassword", "newPassword123")).willReturn(updatedUser);

		mockMvc.perform(put("/users/me/password")
				.header("Authorization", "Bearer testToken")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"currentPassword\":\"oldPassword\",\"newPassword\":\"newPassword123\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id", is(updatedUser.getId().intValue())))
				.andExpect(jsonPath("$.username", is(updatedUser.getUsername())))
				.andExpect(jsonPath("$.token", is(updatedUser.getToken())));
	public void registerUser_successful_returnsCreated() throws Exception {
		User registeredUser = new User();
		registeredUser.setId(1L);
		registeredUser.setUsername("newUser");
		registeredUser.setToken("token-123");
		registeredUser.setStatus(UserStatus.ONLINE);

		given(userService.registerUser(org.mockito.ArgumentMatchers.any(User.class))).willReturn(registeredUser);

		MockHttpServletRequestBuilder postRequest = post("/users/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"username\":\"newUser\",\"password\":\"pw\",\"bio\":\"hello\"}");

		mockMvc.perform(postRequest)
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id", is(1)))
				.andExpect(jsonPath("$.username", is("newUser")))
				.andExpect(jsonPath("$.status", is("ONLINE")));
	}
}