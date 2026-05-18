package ch.uzh.ifi.hase.soprafs26.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import ch.uzh.ifi.hase.soprafs26.entity.Activity;
import ch.uzh.ifi.hase.soprafs26.entity.Destination;
import ch.uzh.ifi.hase.soprafs26.entity.Trip;
import ch.uzh.ifi.hase.soprafs26.entity.TripMembership;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.ActivityRepository;
import ch.uzh.ifi.hase.soprafs26.repository.DestinationRepository;
import ch.uzh.ifi.hase.soprafs26.repository.TripMembershipRepository;
import ch.uzh.ifi.hase.soprafs26.repository.TripRepository;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;

@SpringBootTest
@Transactional
public class ActivityPersistenceIntegrationTest {

    @Autowired
    private ActivityManagementService activityManagementService;

    @Autowired
    private ActivityRepository activityRepository;

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private DestinationRepository destinationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TripMembershipRepository tripMembershipRepository;

    @BeforeEach
    public void setup() {
        // clean database tables used in this test
        activityRepository.deleteAll();
        destinationRepository.deleteAll();
        tripMembershipRepository.deleteAll();
        tripRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    public void addThenFetchActivity_success() {
        // Create user
        User user = new User();
        user.setUsername("integration_user");
        user.setPassword("pwd");
        user.setStatus(ch.uzh.ifi.hase.soprafs26.constant.UserStatus.OFFLINE);
        user.setCreationDate("2026-05-16");
        user = userRepository.save(user);

        // Create trip
        Trip trip = new Trip("Test Trip", "ROOM123", user.getId());
        trip = tripRepository.save(trip);

        // Add membership so tripService considers user a participant
        TripMembership membership = new TripMembership(trip.getId(), user.getId());
        tripMembershipRepository.save(membership);

        // Create destination
        Destination destination = new Destination();
        destination.setTripId(trip.getId());
        destination.setDestinationName("Test Destination");
        destination.setProposedByUserId(user.getId());
        destination = destinationRepository.save(destination);

        // Prepare activity input
        Activity input = new Activity();
        input.setPlaceId("place-1");
        input.setName("Museum");
        input.setAddress("Main St");
        input.setRating(4.5);
        input.setPhotoUrl("http://example.com/photo.jpg");
        input.setLatitude(47.0);
        input.setLongitude(8.0);

        // Persist via service
        Activity saved = activityManagementService.addActivity(trip.getId(), destination.getId(), user.getId(), input);

        assertNotNull(saved.getId());
        assertEquals(trip.getId(), saved.getTripId());
        assertEquals(destination.getId(), saved.getDestinationId());
        assertEquals(user.getId(), saved.getCreatedBy());

        // Fetch via service
        var list = activityManagementService.getSelectedActivities(trip.getId(), destination.getId());
        assertEquals(1, list.size());
        Activity fetched = list.get(0);
        assertEquals(saved.getId(), fetched.getId());
        assertEquals(saved.getPlaceId(), fetched.getPlaceId());
        assertEquals(saved.getName(), fetched.getName());
    }
}
