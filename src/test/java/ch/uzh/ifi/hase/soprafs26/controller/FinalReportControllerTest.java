package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.rest.dto.FinalReportGetDTO;
import ch.uzh.ifi.hase.soprafs26.service.DestinationRealtimeService;
import ch.uzh.ifi.hase.soprafs26.service.FinalReportService;
import ch.uzh.ifi.hase.soprafs26.service.TripService;
import ch.uzh.ifi.hase.soprafs26.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class FinalReportControllerTest {

    @Mock
    private TripService tripService;

    @Mock
    private UserService userService;

    @Mock
    private DestinationRealtimeService destinationRealtimeService;

    @Mock
    private FinalReportService finalReportService;

    private MockMvc mockMvc;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        TripController tripController = new TripController(
                tripService,
                userService,
                destinationRealtimeService,
                finalReportService
        );
        mockMvc = MockMvcBuilders.standaloneSetup(tripController).build();
    }

    @Test
    public void getFinalReport_success() throws Exception {
        User user = new User();
        user.setId(1L);

        FinalReportGetDTO report = new FinalReportGetDTO();
        report.setTripId(1L);
        report.setTripName("Paris Vacation");
        report.setRoomCode("ABC123");

        FinalReportGetDTO.WinningDestinationDTO destination = new FinalReportGetDTO.WinningDestinationDTO();
        destination.setId(10L);
        destination.setName("Paris");

        FinalReportGetDTO.ActivityFinalOutcomeDTO activity = new FinalReportGetDTO.ActivityFinalOutcomeDTO();
        activity.setId(100L);
        activity.setName("Eiffel Tower");
        activity.setRank(1);
        activity.setScore(2L);
        destination.setActivities(List.of(activity));

        report.setWinningDestination(destination);

        Mockito.when(userService.validateToken("Bearer token")).thenReturn(user);
        Mockito.when(finalReportService.getFinalReport(1L, 1L)).thenReturn(report);

        mockMvc.perform(get("/trips/1/final-report")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tripId").value(1))
                .andExpect(jsonPath("$.winningDestination.name").value("Paris"))
                .andExpect(jsonPath("$.winningDestination.activities[0].name").value("Eiffel Tower"));
    }

    @Test
    public void getFinalReport_nonParticipant_forbidden() throws Exception {
        User user = new User();
        user.setId(1L);

        Mockito.when(userService.validateToken("Bearer token")).thenReturn(user);
        Mockito.when(finalReportService.getFinalReport(1L, 1L))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "User is not a participant of this trip"));

        mockMvc.perform(get("/trips/1/final-report")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isForbidden());
    }

    @Test
    public void getFinalReport_tripNotFinalized_conflict() throws Exception {
        User user = new User();
        user.setId(1L);

        Mockito.when(userService.validateToken("Bearer token")).thenReturn(user);
        Mockito.when(finalReportService.getFinalReport(1L, 1L))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Final report is only available for finalized trips"));

        mockMvc.perform(get("/trips/1/final-report")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isConflict());
    }
}