package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.service.UserService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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
}