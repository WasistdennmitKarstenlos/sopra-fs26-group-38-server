package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.entity.Destination;
import ch.uzh.ifi.hase.soprafs26.repository.DestinationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@Transactional
public class DestinationService {

    private final DestinationRepository destinationRepository;
    private final TripService tripService;

    public DestinationService(DestinationRepository destinationRepository, TripService tripService) {
        this.destinationRepository = destinationRepository;
        this.tripService = tripService;
    }

    public List<Destination> getDestinations(Long tripId) {
        tripService.getTripById(tripId);
        return destinationRepository.findByTripIdOrderByIdDesc(tripId);
    }

    public Destination createDestination(Long tripId, Destination destination) {
        tripService.ensureTripIsActiveForMutations(tripId);
        destination.setTripId(tripId);
        validate(destination);

        return destinationRepository.save(destination);
    }

    public Destination updateDestination(Long tripId, Long destinationId, Destination destinationUpdate) {
        tripService.ensureTripIsActiveForMutations(tripId);
        validate(destinationUpdate);
        Destination destination = getDestinationEntity(tripId, destinationId);
        destination.setDestinationName(destinationUpdate.getDestinationName().trim());
        return destinationRepository.save(destination);
    }

    public void deleteDestination(Long tripId, Long destinationId) {
        tripService.ensureTripIsActiveForMutations(tripId);
        Destination destination = getDestinationEntity(tripId, destinationId);
        destinationRepository.delete(destination);
    }

    public Destination getDestinationEntity(Long tripId, Long destinationId) {
        tripService.getTripById(tripId);
        return destinationRepository.findByIdAndTripId(destinationId, tripId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Destination not found"));
    }

    private void validate(Destination destination) {
        if (destination == null || destination.getDestinationName() == null || destination.getDestinationName().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Destination name cannot be empty");
        }
    }
}