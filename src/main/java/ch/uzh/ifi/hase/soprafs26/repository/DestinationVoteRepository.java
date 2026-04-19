package ch.uzh.ifi.hase.soprafs26.repository;

import ch.uzh.ifi.hase.soprafs26.entity.DestinationVote;
import ch.uzh.ifi.hase.soprafs26.entity.VoteType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository("destinationVoteRepository")
public interface DestinationVoteRepository extends JpaRepository<DestinationVote, Long> {

    Optional<DestinationVote> findByDestinationIdAndUserId(Long destinationId, Long userId);

    long countByDestinationIdAndVoteType(Long destinationId, VoteType voteType);
}
