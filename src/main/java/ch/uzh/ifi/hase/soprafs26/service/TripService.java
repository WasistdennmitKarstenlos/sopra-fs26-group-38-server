package ch.uzh.ifi.hase.soprafs26.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ch.uzh.ifi.hase.soprafs26.entity.Trip;
import ch.uzh.ifi.hase.soprafs26.repository.TripRepository;

import java.util.List;

@Service
@Transactional
public class TripService {

	private final TripRepository tripRepository;

	public TripService(@Qualifier("tripRepository") TripRepository tripRepository) {
		this.tripRepository = tripRepository;
	}

	public List<Trip> getTrips() {
		return this.tripRepository.findAll();
	}
}
