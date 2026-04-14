package ch.uzh.ifi.hase.soprafs26.rest.mapper;

import org.junit.jupiter.api.Test;

import ch.uzh.ifi.hase.soprafs26.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs26.entity.Destination;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.rest.dto.DestinationGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.DestinationPostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UserGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UserPostDTO;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
	public void testGetUser_fromUser_toUserGetDTO_success() {
		// create User
		User user = new User();
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
