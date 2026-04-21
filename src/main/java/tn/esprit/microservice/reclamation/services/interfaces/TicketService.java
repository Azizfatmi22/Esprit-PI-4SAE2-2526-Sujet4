package tn.esprit.microservice.reclamation.services.interfaces;

// TicketService.java

import tn.esprit.microservice.reclamation.DTO.TicketResponseDTO;
import tn.esprit.microservice.reclamation.entities.Reclamation;

public interface TicketService {
    TicketResponseDTO createTicket(Reclamation reclamation, String tool);
}