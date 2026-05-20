package ch.uzh.ifi.hase.soprafs26.rest.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class FinalReportGetDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long tripId;
    private String tripName;
    private String roomCode;
    private Date generatedAt;
    private WinningDestinationDTO winningDestination;

    public Long getTripId() {
        return tripId;
    }

    public void setTripId(Long tripId) {
        this.tripId = tripId;
    }

    public String getTripName() {
        return tripName;
    }

    public void setTripName(String tripName) {
        this.tripName = tripName;
    }

    public String getRoomCode() {
        return roomCode;
    }

    public void setRoomCode(String roomCode) {
        this.roomCode = roomCode;
    }

    public Date getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(Date generatedAt) {
        this.generatedAt = generatedAt;
    }

    public WinningDestinationDTO getWinningDestination() {
        return winningDestination;
    }

    public void setWinningDestination(WinningDestinationDTO winningDestination) {
        this.winningDestination = winningDestination;
    }

    public static class WinningDestinationDTO implements Serializable {

        private static final long serialVersionUID = 1L;

        private Long id;
        private String name;
        private long totalUpvotes;
        private long totalDownvotes;
        private double totalScore;
        private List<ActivityFinalOutcomeDTO> activities = new ArrayList<>();

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public long getTotalUpvotes() {
            return totalUpvotes;
        }

        public void setTotalUpvotes(long totalUpvotes) {
            this.totalUpvotes = totalUpvotes;
        }

        public long getTotalDownvotes() {
            return totalDownvotes;
        }

        public void setTotalDownvotes(long totalDownvotes) {
            this.totalDownvotes = totalDownvotes;
        }

        public double getTotalScore() {
            return totalScore;
        }

        public void setTotalScore(double totalScore) {
            this.totalScore = totalScore;
        }

        public List<ActivityFinalOutcomeDTO> getActivities() {
            return activities;
        }

        public void setActivities(List<ActivityFinalOutcomeDTO> activities) {
            this.activities = activities;
        }
    }

    public static class ActivityFinalOutcomeDTO implements Serializable {

        private static final long serialVersionUID = 1L;

        private Long id;
        private String placeId;
        private String name;
        private String address;
        private Double rating;
        private int rank;
        private long upvotes;
        private long downvotes;
        private long score;
        private List<String> comments = new ArrayList<>();

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getPlaceId() {
            return placeId;
        }

        public void setPlaceId(String placeId) {
            this.placeId = placeId;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getAddress() {
            return address;
        }

        public void setAddress(String address) {
            this.address = address;
        }

        public Double getRating() {
            return rating;
        }

        public void setRating(Double rating) {
            this.rating = rating;
        }

        public int getRank() {
            return rank;
        }

        public void setRank(int rank) {
            this.rank = rank;
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

        public List<String> getComments() {
            return comments;
        }

        public void setComments(List<String> comments) {
            this.comments = comments;
        }
    }
}