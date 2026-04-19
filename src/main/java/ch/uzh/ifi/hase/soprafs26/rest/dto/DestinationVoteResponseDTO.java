package ch.uzh.ifi.hase.soprafs26.rest.dto;

import java.io.Serializable;

public class DestinationVoteResponseDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long destinationId;
    private long upvotes;
    private long downvotes;
    private long score;
    private String userVote;

    public Long getDestinationId() {
        return destinationId;
    }

    public void setDestinationId(Long destinationId) {
        this.destinationId = destinationId;
    }

    public long getUpvotes() {
        return upvotes;
    }

    public void setUpvotes(long upvotes) {
        this.upvotes = upvotes;
    }

    public long getDownvotes() {
        return downvotes;
    }

    public void setDownvotes(long downvotes) {
        this.downvotes = downvotes;
    }

    public long getScore() {
        return score;
    }

    public void setScore(long score) {
        this.score = score;
    }

    public String getUserVote() {
        return userVote;
    }

    public void setUserVote(String userVote) {
        this.userVote = userVote;
    }
}
