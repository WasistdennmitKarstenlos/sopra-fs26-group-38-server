package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.entity.Destination;
import ch.uzh.ifi.hase.soprafs26.entity.DestinationVote;
import ch.uzh.ifi.hase.soprafs26.entity.Trip;
import ch.uzh.ifi.hase.soprafs26.entity.VoteType;
import ch.uzh.ifi.hase.soprafs26.repository.DestinationRepository;
import ch.uzh.ifi.hase.soprafs26.repository.DestinationVoteRepository;
import ch.uzh.ifi.hase.soprafs26.rest.dto.DestinationGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.DestinationVoteRequestDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.DestinationVoteResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class DestinationService {

    private final DestinationRepository destinationRepository;
    private final DestinationVoteRepository destinationVoteRepository;
    private final TripService tripService;

    public DestinationService(DestinationRepository destinationRepository,
                              DestinationVoteRepository destinationVoteRepository,
                              TripService tripService) {
        this.destinationRepository = destinationRepository;
        this.destinationVoteRepository = destinationVoteRepository;
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

    public void populateDestinationVoteData(Destination destination, Long userId, DestinationGetDTO dto) {
        long upvotes = destinationVoteRepository.countByDestinationIdAndVoteType(destination.getId(), VoteType.UP);
        long downvotes = destinationVoteRepository.countByDestinationIdAndVoteType(destination.getId(), VoteType.DOWN);
        Optional<DestinationVote> userVote = destinationVoteRepository.findByDestinationIdAndUserId(destination.getId(), userId);

        dto.setUpvotes(upvotes);
        dto.setDownvotes(downvotes);
        dto.setScore(upvotes - downvotes);
        dto.setUserVote(userVote.map(vote -> vote.getVoteType().name()).orElse(null));
    }

    private DestinationVoteResponseDTO buildVoteResponse(Long destinationId, Long userId) {
        long upvotes = destinationVoteRepository.countByDestinationIdAndVoteType(destinationId, VoteType.UP);
        long downvotes = destinationVoteRepository.countByDestinationIdAndVoteType(destinationId, VoteType.DOWN);
        Optional<DestinationVote> userVote = destinationVoteRepository.findByDestinationIdAndUserId(destinationId, userId);

        DestinationVoteResponseDTO response = new DestinationVoteResponseDTO();
        response.setDestinationId(destinationId);
        response.setUpvotes(upvotes);
        response.setDownvotes(downvotes);
        response.setScore(upvotes - downvotes);
        response.setUserVote(userVote.map(vote -> vote.getVoteType().name()).orElse(null));
        return response;
    }

    private void validate(Destination destination) {
        if (destination == null || destination.getDestinationName() == null || destination.getDestinationName().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Destination name cannot be empty");
        }
    }
}