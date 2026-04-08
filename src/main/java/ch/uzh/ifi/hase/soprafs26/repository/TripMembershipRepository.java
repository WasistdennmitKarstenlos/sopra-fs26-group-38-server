package ch.uzh.ifi.hase.soprafs26.repository;

import ch.uzh.ifi.hase.soprafs26.entity.TripMembership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository("tripMembershipRepository")
public interface TripMembershipRepository extends JpaRepository<TripMembership, Long> {
    boolean existsByTripIdAndUserId(Long tripId, Long userId);
}
