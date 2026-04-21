package tn.esprit.microservice.reclamation.DTO;

// TicketResponseDTO.java

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TicketResponseDTO {
    private String ticketId;
    private String ticketUrl;
    private String toolName;
    private boolean alreadyExists;
}