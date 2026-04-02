package ch.uzh.ifi.hase.soprafs26.rest.mapper;

import org.mapstruct.*;
import org.mapstruct.factory.Mappers;

import ch.uzh.ifi.hase.soprafs26.entity.Trip;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.entity.Trip;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UserGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UserPostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UserRegisterDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.TripGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.TripPostDTO;

/**
 * DTOMapper
 * This class is responsible for generating classes that will automatically
 * transform/map the internal representation
 * of an entity (e.g., the User) to the external/API representation (e.g.,
 * UserGetDTO for getting, UserPostDTO for creating)
 * and vice versa.
 * Additional mappers can be defined for new entities.
 * Always created one mapper for getting information (GET) and one mapper for
 * creating information (POST).
 */
@Mapper
public interface DTOMapper {

	DTOMapper INSTANCE = Mappers.getMapper(DTOMapper.class);

	@Mapping(source = "username", target = "username")
	@Mapping(source = "password", target = "password")
	User convertUserPostDTOtoEntity(UserPostDTO userPostDTO);

	@Mapping(source = "id", target = "id")
	@Mapping(source = "username", target = "username")
	@Mapping(source = "status", target = "status")
	@Mapping(source = "creationDate", target = "creationDate")
	@Mapping(source = "bio", target = "bio")
	@Mapping(source = "token", target = "token")
	UserGetDTO convertEntityToUserGetDTO(User user);

    @Mapping(source = "username", target = "username")
    @Mapping(source = "password", target = "password")
	@Mapping(source = "bio", target = "bio")
	User convertUserRegisterDTOtoEntity(UserRegisterDTO userRegisterDTO);
	@Mapping(source = "name", target = "name")
	Trip convertTripPostDTOtoEntity(TripPostDTO tripPostDTO);

	@Mapping(source = "id", target = "id")
	@Mapping(source = "name", target = "name")
	@Mapping(source = "roomCode", target = "roomCode")
	@Mapping(source = "hostId", target = "hostId")
	@Mapping(source = "creationDate", target = "creationDate")
	@Mapping(source = "status", target = "status", qualifiedByName = "tripStatusToString")
	@Mapping(source = "finalDestinationId", target = "finalDestinationId")
	TripGetDTO convertEntityToTripGetDTO(Trip trip);

	@Named("tripStatusToString")
	default String tripStatusToString(Trip.TripStatus status) {
		return status != null ? status.toString() : null;
	}
}
