package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.entity.Destination;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.rest.dto.DestinationGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.DestinationPostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs26.service.DestinationRealtimeService;
import ch.uzh.ifi.hase.soprafs26.service.DestinationService;
import ch.uzh.ifi.hase.soprafs26.service.TripService;
import ch.uzh.ifi.hase.soprafs26.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
public class DestinationController {

    private final DestinationService destinationService;
    private final TripService tripService;
    private final UserService userService;
    private final DestinationRealtimeService destinationRealtimeService;

    public DestinationController(
            DestinationService destinationService,
            TripService tripService,
            UserService userService,
            DestinationRealtimeService destinationRealtimeService) {
        this.destinationService = destinationService;
        this.tripService = tripService;
        this.userService = userService;
        this.destinationRealtimeService = destinationRealtimeService;
    }

    @GetMapping("/trips/{tripId}/destinations")
    @ResponseStatus(HttpStatus.OK)
    public List<DestinationGetDTO> getDestinations(@PathVariable("tripId") Long tripId,
                                                   @RequestHeader(value = "Authorization", required = false) String token) {
        User requester = userService.validateToken(token);
        return tripService.getDestinations(tripId, requester.getId()).stream()
                .map(DTOMapper.INSTANCE::convertEntityToDestinationGetDTO)
                .collect(Collectors.toList());
    }

    @PostMapping("/trips/{tripId}/destinations")
    @ResponseStatus(HttpStatus.CREATED)
    public DestinationGetDTO createDestination(@PathVariable("tripId") Long tripId,
                                               @RequestBody DestinationPostDTO destinationPostDTO,
                                               @RequestHeader(value = "Authorization", required = false) String token) {
        User requester = userService.validateToken(token);
        Destination destination = DTOMapper.INSTANCE.convertDestinationPostDTOtoEntity(destinationPostDTO);
        Destination savedDestination = tripService.addDestination(tripId, requester.getId(), destination);

        if (destinationRealtimeService != null) {
            List<DestinationGetDTO> sharedList = tripService.getDestinations(tripId, requester.getId()).stream()
                    .map(DTOMapper.INSTANCE::convertEntityToDestinationGetDTO)
                    .collect(Collectors.toList());
            destinationRealtimeService.publish(tripId, sharedList);
        }

        return DTOMapper.INSTANCE.convertEntityToDestinationGetDTO(savedDestination);
    }

    @PutMapping("/trips/{tripId}/destinations/{destinationId}")
    @ResponseStatus(HttpStatus.OK)
    public DestinationGetDTO updateDestination(@PathVariable("tripId") Long tripId,
                                               @PathVariable("destinationId") Long destinationId,
                                               @RequestBody DestinationPostDTO destinationPostDTO,
                                               @RequestHeader(value = "Authorization", required = false) String token) {
        userService.validateToken(token);
        return destinationService.updateDestination(tripId, destinationId, destinationPostDTO);
    }

    @DeleteMapping("/trips/{tripId}/destinations/{destinationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDestination(@PathVariable("tripId") Long tripId,
                                  @PathVariable("destinationId") Long destinationId,
                                  @RequestHeader(value = "Authorization", required = false) String token) {
        userService.validateToken(token);
        destinationService.deleteDestination(tripId, destinationId);
    }
}