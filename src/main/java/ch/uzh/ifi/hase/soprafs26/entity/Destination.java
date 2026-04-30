package ch.uzh.ifi.hase.soprafs26.entity;

import jakarta.persistence.*;

import java.io.Serializable;

@Entity
@Table(name = "DESTINATION", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"tripId", "destinationName"})
})
public class Destination implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long tripId;

    @Column(nullable = false)
    private String destinationName;

    @Column(nullable = false)
    private Long proposedByUserId;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTripId() {
        return tripId;
    }

    public void setTripId(Long tripId) {
        this.tripId = tripId;
    }

    public String getDestinationName() {
        return destinationName;
    }

    public void setDestinationName(String destinationName) {
        this.destinationName = destinationName;
    }

    public Long getProposedByUserId() {
        return proposedByUserId;
    }

    public void setProposedByUserId(Long proposedByUserId) {
        this.proposedByUserId = proposedByUserId;
    }
}
