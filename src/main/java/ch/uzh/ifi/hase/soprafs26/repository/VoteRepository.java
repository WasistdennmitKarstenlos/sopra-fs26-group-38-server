package ch.uzh.ifi.hase.soprafs26.repository;

import ch.uzh.ifi.hase.soprafs26.entity.Vote;
import ch.uzh.ifi.hase.soprafs26.entity.VoteType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository("voteRepository")
public interface VoteRepository extends JpaRepository<Vote, Long> {

    // Find a vote by activity ID and user ID, ensuring that each user can only vote once per activity.
    Optional<Vote> findByActivityIdAndUserId(Long activityId, Long userId);

    // Count votes by activity ID and vote type.
    long countByActivityIdAndVoteType(Long activityId, VoteType voteType);

    // Find all votes for a specific activity.
    List<Vote> findByActivityId(Long activityId);
    
    // Find all votes for a list of activity IDs.
    List<Vote> findByActivityIdIn(List<Long> activityIds);
}
