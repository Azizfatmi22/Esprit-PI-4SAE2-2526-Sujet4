package tn.esprit.microservice.reclamation.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.microservice.reclamation.DTO.TicketResponseDTO;
import tn.esprit.microservice.reclamation.entities.Reclamation;
import tn.esprit.microservice.reclamation.repositories.ReclamationRepository;
import tn.esprit.microservice.reclamation.services.impl.JiraTicketService;
import tn.esprit.microservice.reclamation.services.impl.ReclamationResponseServiceImpl;
import tn.esprit.microservice.reclamation.services.interfaces.TicketService;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/msreclamation/admin")
public class AdminTicketController {

    private static final Logger logger = LoggerFactory.getLogger(AdminTicketController.class);

    // Constantes pour éviter la duplication
    private static final String RECLAMATION_NOT_FOUND = "Réclamation non trouvée";
    private static final String TICKET_ID = "ticketId";
    private static final String TICKET_URL = "ticketUrl";
    private static final String TOOL_NAME = "toolName";
    private static final String ALREADY_EXISTS = "alreadyExists";
    private static final String EXISTS = "exists";
    private static final String ERROR = "error";

    @Autowired
    private ReclamationRepository reclamationRepository;

    @Autowired
    private JiraTicketService jiraTicketService;

    @Autowired
    private ReclamationResponseServiceImpl reclamationService;

    @Autowired
    private TicketService ticketService;

    @Value("${jira.url:}")
    private String jiraUrl;

    @Value("${linear.api.key:}")
    private String linearApiKey;

    @Value("${clickup.api.key:}")
    private String clickupApiKey;

    @PostMapping("/reclamations/{id}/create-ticket")
    public ResponseEntity<Object> createExternalTicket(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> payload) {

        try {
            Reclamation reclamation = reclamationRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException(RECLAMATION_NOT_FOUND));

            if (reclamation.getExternalTicketUrl() != null && !reclamation.getExternalTicketUrl().isEmpty()) {
                return ResponseEntity.ok(Map.of(
                        ALREADY_EXISTS, true,
                        TICKET_ID, reclamation.getExternalTicketId(),
                        TICKET_URL, reclamation.getExternalTicketUrl(),
                        TOOL_NAME, reclamation.getExternalTool()
                ));
            }

            String tool = payload != null && payload.containsKey("tool") ? payload.get("tool") : "demo";
            Map<String, String> result;

            switch (tool) {
                case "jira":
                    try {
                        TicketResponseDTO dto = ticketService.createTicket(reclamation, tool);
                        result = new HashMap<>();
                        result.put(TICKET_ID, dto.getTicketId());
                        result.put(TICKET_URL, dto.getTicketUrl());
                        result.put(TOOL_NAME, dto.getToolName());
                    } catch (Exception e) {
                        logger.error("Erreur Jira: {}", e.getMessage(), e);
                        result = new HashMap<>();
                        result.put(ERROR, "Erreur Jira: " + e.getMessage());
                    }
                    break;
                case "linear":
                    result = createLinearTicket(reclamation);
                    break;
                case "clickup":
                    result = createClickupTicket(reclamation);
                    break;
                default:
                    result = createDemoTicket(reclamation);
                    break;
            }

            if (result.containsKey(ERROR)) {
                return ResponseEntity.status(500).body(result);
            }

            reclamation.setExternalTicketId(result.get(TICKET_ID));
            reclamation.setExternalTicketUrl(result.get(TICKET_URL));
            reclamation.setExternalTool(result.get(TOOL_NAME));
            reclamationRepository.save(reclamation);

            return ResponseEntity.ok(Map.of(
                    TICKET_ID, result.get(TICKET_ID),
                    TICKET_URL, result.get(TICKET_URL),
                    TOOL_NAME, result.get(TOOL_NAME),
                    ALREADY_EXISTS, false
            ));

        } catch (Exception e) {
            logger.error("Erreur création ticket: {}", e.getMessage(), e);
            Map<String, String> error = new HashMap<>();
            error.put(ERROR, e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    @GetMapping("/reclamations/{id}/ticket")
    public ResponseEntity<Object> getTicketInfo(@PathVariable Long id) {
        try {
            Reclamation rec = reclamationRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException(RECLAMATION_NOT_FOUND));

            if (rec.getExternalTicketUrl() != null && !rec.getExternalTicketUrl().isEmpty()) {
                return ResponseEntity.ok(Map.of(
                        EXISTS, true,
                        TICKET_ID, rec.getExternalTicketId(),
                        TICKET_URL, rec.getExternalTicketUrl(),
                        TOOL_NAME, rec.getExternalTool()
                ));
            }
            return ResponseEntity.ok(Map.of(EXISTS, false));
        } catch (Exception e) {
            logger.error("Erreur récupération ticket: {}", e.getMessage(), e);
            return ResponseEntity.ok(Map.of(EXISTS, false));
        }
    }

    @PostMapping("/reclamations/{id}/create-ticket-v2")
    public ResponseEntity<Object> createTicketV2(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        try {
            String tool = body.get("tool");
            Reclamation rec = reclamationRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException(RECLAMATION_NOT_FOUND));

            TicketResponseDTO dto = ticketService.createTicket(rec, tool);

            return ResponseEntity.ok(Map.of(
                    TICKET_ID, dto.getTicketId(),
                    TICKET_URL, dto.getTicketUrl(),
                    TOOL_NAME, dto.getToolName(),
                    ALREADY_EXISTS, false
            ));
        } catch (Exception e) {
            logger.error("Erreur création ticket v2: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of(ERROR, e.getMessage()));
        }
    }

    private Map<String, String> createDemoTicket(Reclamation rec) {
        String demoId = "DEMO-" + rec.getId();
        Map<String, String> result = new HashMap<>();
        result.put(TICKET_ID, demoId);
        result.put(TICKET_URL, "https://demo-linear.app/issue/" + demoId);
        result.put(TOOL_NAME, "Démo");
        return result;
    }

    private Map<String, String> createLinearTicket(Reclamation rec) {
        Map<String, String> result = new HashMap<>();
        if (linearApiKey == null || linearApiKey.isEmpty()) {
            result.put(ERROR, "Linear non configuré");
            return result;
        }
        result.put(TICKET_ID, "LIN-" + rec.getId());
        result.put(TICKET_URL, "https://linear.app/issue/LIN-" + rec.getId());
        result.put(TOOL_NAME, "Linear");
        return result;
    }

    private Map<String, String> createClickupTicket(Reclamation rec) {
        Map<String, String> result = new HashMap<>();
        if (clickupApiKey == null || clickupApiKey.isEmpty()) {
            result.put(ERROR, "ClickUp non configuré");
            return result;
        }
        result.put(TICKET_ID, "CUP-" + rec.getId());
        result.put(TICKET_URL, "https://app.clickup.com/task/CUP-" + rec.getId());
        result.put(TOOL_NAME, "ClickUp");
        return result;
    }
}