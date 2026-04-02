package ch.uzh.ifi.hase.soprafs26.repository;

import ch.uzh.ifi.hase.soprafs26.entity.Destination;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository("destinationRepository")
public interface DestinationRepository extends JpaRepository<Destination, Long> {
    List<Destination> findByTripIdOrderByCreatedAtAsc(Long tripId);
}
