package ch.uzh.ifi.hase.soprafs26.rest.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

import ch.uzh.ifi.hase.soprafs26.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs26.entity.Destination;
import ch.uzh.ifi.hase.soprafs26.entity.Trip;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.rest.dto.DestinationGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.DestinationPostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.TripGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.TripPostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UserGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UserPostDTO;

/**
 * DTOMapperTest
 * Tests if the mapping between the internal and the external/API representation
 * works.
 */
public class DTOMapperTest {
	@Test
	public void testCreateUser_fromUserPostDTO_toUser_success() {
		// create UserPostDTO
		UserPostDTO userPostDTO = new UserPostDTO();
		userPostDTO.setUsername("username");

		// MAP -> Create user
		User user = DTOMapper.INSTANCE.convertUserPostDTOtoEntity(userPostDTO);

		// check content
		assertEquals(userPostDTO.getUsername(), user.getUsername());
	}

	@Test
	public void testCreateUser_fromUserPostDTO_toUser_withNullUsername() {
		UserPostDTO userPostDTO = new UserPostDTO();

		User user = DTOMapper.INSTANCE.convertUserPostDTOtoEntity(userPostDTO);

		assertEquals(null, user.getUsername());
	}

	@Test
	public void testGetUser_fromUser_toUserGetDTO_success() {
		// create User
		User user = new User();
		user.setId(1L);
		user.setUsername("firstname@lastname");
		user.setStatus(UserStatus.OFFLINE);
		user.setToken("1");

		// MAP -> Create UserGetDTO
		UserGetDTO userGetDTO = DTOMapper.INSTANCE.convertEntityToUserGetDTO(user);

		// check content
		assertEquals(user.getId(), userGetDTO.getId());
		assertEquals(user.getUsername(), userGetDTO.getUsername());
		assertEquals(user.getStatus(), userGetDTO.getStatus());
	}

	@Test
	public void testCreateTrip_fromTripPostDTO_mapsImageBase64() {
		TripPostDTO tripPostDTO = new TripPostDTO();
		tripPostDTO.setName("Beach Trip");
		tripPostDTO.setImageBase64("data:image/jpeg;base64,/9j/abc123");

		Trip trip = DTOMapper.INSTANCE.convertTripPostDTOtoEntity(tripPostDTO);

		assertEquals(tripPostDTO.getImageBase64(), trip.getImageBase64());
	}

	@Test
	public void testGetTrip_fromEntity_mapsImageBase64() {
		Trip trip = new Trip();
		trip.setId(1L);
		trip.setName("Beach Trip");
		trip.setRoomCode("XYZ789");
		trip.setHostId(1L);
		trip.setStatus(Trip.TripStatus.ACTIVE);
		trip.setImageBase64("data:image/jpeg;base64,/9j/abc123");

		TripGetDTO tripGetDTO = DTOMapper.INSTANCE.convertEntityToTripGetDTO(trip);

		assertEquals(trip.getImageBase64(), tripGetDTO.getImageBase64());
	}

	@Test
	public void testGetTrip_fromEntity_nullImageBase64_mapsAsNull() {
		Trip trip = new Trip();
		trip.setId(1L);
		trip.setName("Beach Trip");
		trip.setRoomCode("XYZ789");
		trip.setHostId(1L);
		trip.setStatus(Trip.TripStatus.ACTIVE);

		TripGetDTO tripGetDTO = DTOMapper.INSTANCE.convertEntityToTripGetDTO(trip);

		assertEquals(null, tripGetDTO.getImageBase64());
	}

	@Test
	public void testCreateDestination_fromDestinationPostDTO_toEntity_success() {
		DestinationPostDTO destinationPostDTO = new DestinationPostDTO();
		destinationPostDTO.setDestinationName("Zurich");

		Destination destination = DTOMapper.INSTANCE.convertDestinationPostDTOtoEntity(destinationPostDTO);

		assertEquals(destinationPostDTO.getDestinationName(), destination.getDestinationName());
	}

	@Test
	public void testGetDestination_fromEntity_toDestinationGetDTO_success() {
		Destination destination = new Destination();
		destination.setId(1L);
		destination.setTripId(2L);
		destination.setDestinationName("Bern");
		destination.setProposedByUserId(3L);

		DestinationGetDTO destinationGetDTO = DTOMapper.INSTANCE.convertEntityToDestinationGetDTO(destination);

		assertEquals(destination.getId(), destinationGetDTO.getId());
		assertEquals(destination.getTripId(), destinationGetDTO.getTripId());
		assertEquals(destination.getDestinationName(), destinationGetDTO.getDestinationName());
		assertEquals(destination.getProposedByUserId(), destinationGetDTO.getProposedByUserId());
	}
}
