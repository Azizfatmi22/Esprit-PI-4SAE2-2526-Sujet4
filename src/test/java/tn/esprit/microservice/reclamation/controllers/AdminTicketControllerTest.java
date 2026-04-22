package tn.esprit.microservice.reclamation.controllers;


import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.ResponseEntity;
import tn.esprit.microservice.reclamation.entities.Reclamation;
import tn.esprit.microservice.reclamation.repositories.ReclamationRepository;
import tn.esprit.microservice.reclamation.services.impl.JiraTicketService;
import tn.esprit.microservice.reclamation.services.interfaces.TicketService;
import tn.esprit.microservice.reclamation.DTO.TicketResponseDTO;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
class AdminTicketControllerTest {

    @Mock
    private ReclamationRepository reclamationRepository;

    @Mock
    private TicketService ticketService;

    @InjectMocks
    private AdminTicketController controller;

    @Test
    void testCreateTicket_success() {

        Reclamation rec = Reclamation.builder()
                .id(1L)
                .build();

        when(reclamationRepository.findById(1L))
                .thenReturn(Optional.of(rec));

        when(ticketService.createTicket(any(), anyString()))
                .thenReturn(new TicketResponseDTO(
                        "JIRA-1",
                        "http://jira/browse/JIRA-1",
                        "Jira",
                        false
                ));

        ResponseEntity<?> response =
                controller.createExternalTicket(1L, Map.of("tool", "jira"));

        assertEquals(200, response.getStatusCodeValue());
    }
}