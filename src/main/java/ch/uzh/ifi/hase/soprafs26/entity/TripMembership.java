package ch.uzh.ifi.hase.soprafs26.entity;

import jakarta.persistence.*;

import java.io.Serializable;

@Entity
@Table(name = "TRIPMEMBERSHIP", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"tripId", "userId"})
})
public class TripMembership implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long tripId;

    @Column(nullable = false)
    private Long userId;

    public TripMembership() {
        // JPA default constructor
    }

    public TripMembership(Long tripId, Long userId) {
        this.tripId = tripId;
        this.userId = userId;
    }

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

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}
