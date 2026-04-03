package ch.uzh.ifi.hase.soprafs26.repository;

import ch.uzh.ifi.hase.soprafs26.entity.Activity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository("activityRepository")
public interface ActivityRepository extends JpaRepository<Activity, Long> {
    List<Activity> findByTripIdAndDestinationIdOrderByIdDesc(Long tripId, Long destinationId);

    Optional<Activity> findByTripIdAndDestinationIdAndPlaceId(Long tripId, Long destinationId, String placeId);

    Optional<Activity> findByIdAndTripIdAndDestinationId(Long activityId, Long tripId, Long destinationId);
}