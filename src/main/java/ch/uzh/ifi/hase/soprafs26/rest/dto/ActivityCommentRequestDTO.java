package ch.uzh.ifi.hase.soprafs26.rest.dto;

import java.io.Serializable;

public class ActivityCommentRequestDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String comment;

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
