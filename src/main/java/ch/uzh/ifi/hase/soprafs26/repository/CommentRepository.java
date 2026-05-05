package ch.uzh.ifi.hase.soprafs26.repository;

import ch.uzh.ifi.hase.soprafs26.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository("commentRepository")
public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findByTripIdAndDestinationIdAndActivityIdOrderByCreatedAtAsc(Long tripId, Long destinationId, Long activityId);

    List<Comment> findByActivityIdOrderByCreatedAtAsc(Long activityId);
}