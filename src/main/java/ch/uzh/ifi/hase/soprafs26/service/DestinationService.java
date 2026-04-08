package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.entity.Destination;
import ch.uzh.ifi.hase.soprafs26.repository.DestinationRepository;
import ch.uzh.ifi.hase.soprafs26.rest.dto.DestinationGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.DestinationPostDTO;
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

    public List<DestinationGetDTO> getDestinations(Long tripId) {
        tripService.getTripById(tripId);
        return destinationRepository.findByTripIdOrderByIdDesc(tripId)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    public DestinationGetDTO createDestination(Long tripId, DestinationPostDTO destinationPostDTO) {
        tripService.getTripById(tripId);
        validate(destinationPostDTO);

        Destination destination = new Destination();
        destination.setTripId(tripId);
        destination.setDestinationName(destinationPostDTO.getDestinationName().trim());

        return toDTO(destinationRepository.save(destination));
    }

    public DestinationGetDTO updateDestination(Long tripId, Long destinationId, DestinationPostDTO destinationPostDTO) {
        validate(destinationPostDTO);
        Destination destination = getDestinationEntity(tripId, destinationId);
        destination.setDestinationName(destinationPostDTO.getDestinationName().trim());
        return toDTO(destinationRepository.save(destination));
    }

    public void deleteDestination(Long tripId, Long destinationId) {
        Destination destination = getDestinationEntity(tripId, destinationId);
        destinationRepository.delete(destination);
    }

    public Destination getDestinationEntity(Long tripId, Long destinationId) {
        tripService.getTripById(tripId);
        return destinationRepository.findByIdAndTripId(destinationId, tripId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Destination not found"));
    }

    private void validate(DestinationPostDTO destinationPostDTO) {
        if (destinationPostDTO == null || destinationPostDTO.getDestinationName() == null || destinationPostDTO.getDestinationName().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Destination name cannot be empty");
        }
    }

    private DestinationGetDTO toDTO(Destination destination) {
        DestinationGetDTO dto = new DestinationGetDTO();
        dto.setId(destination.getId());
        dto.setTripId(destination.getTripId());
        dto.setDestinationName(destination.getDestinationName());
        return dto;
    }
}