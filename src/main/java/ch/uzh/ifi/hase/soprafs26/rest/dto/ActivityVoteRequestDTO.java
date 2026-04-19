package ch.uzh.ifi.hase.soprafs26.rest.dto;

import java.io.Serializable;

public class ActivityVoteRequestDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String voteType;

    public String getVoteType() {
        return voteType;
    }

    public void setVoteType(String voteType) {
        this.voteType = voteType;
    }
}
