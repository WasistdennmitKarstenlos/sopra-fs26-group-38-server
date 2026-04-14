package ch.uzh.ifi.hase.soprafs26.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ch.uzh.ifi.hase.soprafs26.entity.TripMembership;

@Repository("tripMembershipRepository")
public interface TripMembershipRepository extends JpaRepository<TripMembership, Long> {
    boolean existsByTripIdAndUserId(Long tripId, Long userId);

    List<TripMembership> findByUserId(Long userId);
}
