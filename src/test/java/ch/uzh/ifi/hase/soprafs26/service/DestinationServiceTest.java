package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.entity.Destination;
import ch.uzh.ifi.hase.soprafs26.repository.DestinationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class DestinationServiceTest {

    @Mock
    private DestinationRepository destinationRepository;

    @Mock
    private TripService tripService;

    @InjectMocks
    private DestinationService destinationService;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void createDestination_readOnlyMode_badRequest() {
        Destination destination = new Destination();
        destination.setDestinationName("Zurich");

        Mockito.doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "read-only"))
                .when(tripService).ensureTripIsActiveForMutations(1L);

        assertThrows(ResponseStatusException.class, () -> destinationService.createDestination(1L, destination));
        Mockito.verifyNoInteractions(destinationRepository);
    }

    @Test
    public void updateDestination_readOnlyMode_badRequest() {
        Destination update = new Destination();
        update.setDestinationName("Basel");

        Mockito.doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "read-only"))
                .when(tripService).ensureTripIsActiveForMutations(1L);

        assertThrows(ResponseStatusException.class, () -> destinationService.updateDestination(1L, 11L, update));
        Mockito.verifyNoInteractions(destinationRepository);
    }

    @Test
    public void deleteDestination_readOnlyMode_badRequest() {
        Mockito.doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "read-only"))
                .when(tripService).ensureTripIsActiveForMutations(1L);

        assertThrows(ResponseStatusException.class, () -> destinationService.deleteDestination(1L, 11L));
        Mockito.verifyNoInteractions(destinationRepository);
    }

    @Test
    public void updateDestination_activeMode_success() {
        Destination existing = new Destination();
        existing.setId(11L);
        existing.setTripId(1L);
        existing.setDestinationName("Old Name");

        Destination update = new Destination();
        update.setDestinationName("New Name");

        Mockito.when(destinationRepository.findByIdAndTripId(11L, 1L)).thenReturn(Optional.of(existing));
        Mockito.when(destinationRepository.save(Mockito.any(Destination.class))).thenReturn(existing);

        destinationService.updateDestination(1L, 11L, update);

        Mockito.verify(destinationRepository, Mockito.times(1)).save(existing);
    }
}
