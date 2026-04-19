package ch.uzh.ifi.hase.soprafs26.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ch.uzh.ifi.hase.soprafs26.entity.Trip;

@Repository("tripRepository")
public interface TripRepository extends JpaRepository<Trip, Long> {
    /**
     * Find a trip by its unique room code
     * @param roomCode the unique code to access the trip room
     * @return Optional containing the trip if found
     */
    Optional<Trip> findByRoomCode(String roomCode);

    /**
     * Find all trips where the given user is the host
     * @param hostId the ID of the host user
     * @return list of trips hosted by the user
     */
    List<Trip> findByHostId(Long hostId);

    /**
     * Find a trip by its name and host
     * @param name the trip name
     * @param hostId the host user ID
     * @return Optional containing the trip if found
     */
    Optional<Trip> findByNameAndHostId(String name, Long hostId);

    /**
     * Find all trips whose IDs are in the given list, ordered newest first
     * @param ids list of trip IDs
     * @return trips ordered by creation date descending
     */
    List<Trip> findByIdInOrderByCreationDateDesc(List<Long> ids);
}
