package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.rest.dto.DestinationGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.DestinationPostDTO;
import ch.uzh.ifi.hase.soprafs26.service.DestinationService;
import ch.uzh.ifi.hase.soprafs26.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class DestinationControllerTest {

    @Mock
    private DestinationService destinationService;

    @Mock
    private UserService userService;

    private MockMvc mockMvc;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        DestinationController destinationController = new DestinationController(destinationService, userService);
        mockMvc = MockMvcBuilders.standaloneSetup(destinationController).build();
    }

    @Test
    public void getDestinations_success() throws Exception {
        User user = new User();
        user.setId(1L);

        DestinationGetDTO dto = new DestinationGetDTO();
        dto.setId(11L);
        dto.setTripId(1L);
        dto.setDestinationName("Zurich");

        Mockito.when(userService.validateToken("Bearer token")).thenReturn(user);
        Mockito.when(destinationService.getDestinations(1L)).thenReturn(List.of(dto));

        mockMvc.perform(get("/trips/1/destinations")
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(11))
                .andExpect(jsonPath("$[0].destinationName").value("Zurich"));
    }

    @Test
    public void createDestination_success() throws Exception {
        User user = new User();
        user.setId(1L);

        DestinationPostDTO postDTO = new DestinationPostDTO();
        postDTO.setDestinationName("Zurich");

        DestinationGetDTO saved = new DestinationGetDTO();
        saved.setId(11L);
        saved.setTripId(1L);
        saved.setDestinationName("Zurich");

        Mockito.when(userService.validateToken("Bearer token")).thenReturn(user);
        Mockito.when(destinationService.createDestination(Mockito.eq(1L), Mockito.any(DestinationPostDTO.class))).thenReturn(saved);

        mockMvc.perform(post("/trips/1/destinations")
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(postDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(11))
                .andExpect(jsonPath("$.destinationName").value("Zurich"));
    }
}