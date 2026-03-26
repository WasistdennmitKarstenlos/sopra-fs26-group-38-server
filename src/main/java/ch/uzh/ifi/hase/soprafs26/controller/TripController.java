package ch.uzh.ifi.hase.soprafs26.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ch.uzh.ifi.hase.soprafs26.entity.Trip;
import ch.uzh.ifi.hase.soprafs26.rest.dto.TripGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs26.service.TripService;
import ch.uzh.ifi.hase.soprafs26.service.UserService;

import java.util.ArrayList;
import java.util.List;

/**
 * Trip Controller
 * This class is responsible for handling all REST requests related to trips.
 */
@RestController
public class TripController {

	private final TripService tripService;
	private final UserService userService;

	TripController(TripService tripService, UserService userService) {
		this.tripService = tripService;
		this.userService = userService;
	}

	@GetMapping("/trips")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<TripGetDTO> getAllTrips(
			@RequestHeader(value = "Authorization", required = false) String token) {
		// Only authenticated users may access trip overview.
		userService.validateToken(token);

		List<Trip> trips = tripService.getTrips();
		List<TripGetDTO> tripGetDTOs = new ArrayList<>();

		for (Trip trip : trips) {
			tripGetDTOs.add(DTOMapper.INSTANCE.convertEntityToTripGetDTO(trip));
		}
		return tripGetDTOs;
	}
}
