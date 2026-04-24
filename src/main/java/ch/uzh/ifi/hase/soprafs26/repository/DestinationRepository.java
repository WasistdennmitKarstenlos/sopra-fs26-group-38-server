package ch.uzh.ifi.hase.soprafs26.repository;

import ch.uzh.ifi.hase.soprafs26.entity.Destination;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository("destinationRepository")
public interface DestinationRepository extends JpaRepository<Destination, Long> {
    List<Destination> findByTripIdOrderByIdDesc(Long tripId);

    boolean existsByTripId(Long tripId);

    Optional<Destination> findByIdAndTripId(Long destinationId, Long tripId);
}
