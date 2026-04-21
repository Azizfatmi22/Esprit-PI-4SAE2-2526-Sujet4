package tn.esprit.microservice.reclamation.controllers;

// AdminTicketController.java

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
//@CrossOrigin(origins = "http://localhost:4200")
public class AdminTicketController {

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
    public ResponseEntity<?> createExternalTicket(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> payload) {

        try {
            Reclamation reclamation = reclamationRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Réclamation non trouvée"));

            if (reclamation.getExternalTicketUrl() != null && !reclamation.getExternalTicketUrl().isEmpty()) {
                return ResponseEntity.ok(Map.of(
                        "alreadyExists", true,
                        "ticketId", reclamation.getExternalTicketId(),
                        "ticketUrl", reclamation.getExternalTicketUrl(),
                        "toolName", reclamation.getExternalTool()
                ));
            }

            String tool = payload != null && payload.containsKey("tool") ? payload.get("tool") : "demo";

            Map<String, String> result;

            switch (tool) {
                case "jira":
                    try {
                        TicketResponseDTO dto = ticketService.createTicket(reclamation, tool);

                        result = new HashMap<>();
                        result.put("ticketId", dto.getTicketId());
                        result.put("ticketUrl", dto.getTicketUrl());
                        result.put("toolName", dto.getToolName());

                    } catch (Exception e) {
                        result = new HashMap<>();
                        result.put("error", "Erreur Jira: " + e.getMessage());
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

            if (result.containsKey("error")) {
                return ResponseEntity.status(500).body(result);
            }

            reclamation.setExternalTicketId(result.get("ticketId"));
            reclamation.setExternalTicketUrl(result.get("ticketUrl"));
            reclamation.setExternalTool(result.get("toolName"));
            reclamationRepository.save(reclamation);

            return ResponseEntity.ok(Map.of(
                    "ticketId", result.get("ticketId"),
                    "ticketUrl", result.get("ticketUrl"),
                    "toolName", result.get("toolName"),
                    "alreadyExists", false
            ));

        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
    @GetMapping("/reclamations/{id}/ticket")
    public ResponseEntity<?> getTicketInfo(@PathVariable Long id) {
        try {
            Reclamation rec = reclamationRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Réclamation non trouvée"));

            if (rec.getExternalTicketUrl() != null && !rec.getExternalTicketUrl().isEmpty()) {
                return ResponseEntity.ok(Map.of(
                        "exists", true,
                        "ticketId", rec.getExternalTicketId(),
                        "ticketUrl", rec.getExternalTicketUrl(),
                        "toolName", rec.getExternalTool()
                ));
            }
            return ResponseEntity.ok(Map.of("exists", false));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("exists", false));
        }
    }

    // Mode démo - pas besoin d'API externe
    private Map<String, String> createDemoTicket(Reclamation rec) {
        String demoId = "DEMO-" + rec.getId();
        String demoUrl = "https://demo-linear.app/issue/" + demoId;

        Map<String, String> result = new HashMap<>();
        result.put("ticketId", demoId);
        result.put("ticketUrl", demoUrl);
        result.put("toolName", "Démo");
        return result;
    }

    private Map<String, String> createJiraTicket(Reclamation rec) {
        Map<String, String> result = new HashMap<>();

        // Vérifier que Jira est configuré
        if (jiraUrl == null || jiraUrl.isEmpty()) {
            result.put("error", "Jira non configuré. Vérifiez jira.url dans application.properties");
            return result;
        }

        // Ici votre code Jira existant
        String ticketId = "JIRA-" + rec.getId();
        String ticketUrl = jiraUrl + "/browse/" + ticketId;

        result.put("ticketId", ticketId);
        result.put("ticketUrl", ticketUrl);
        result.put("toolName", "Jira");
        return result;
    }

    private Map<String, String> createLinearTicket(Reclamation rec) {
        Map<String, String> result = new HashMap<>();

        if (linearApiKey == null || linearApiKey.isEmpty()) {
            result.put("error", "Linear non configuré. Vérifiez linear.api.key dans application.properties");
            return result;
        }

        String ticketId = "LIN-" + rec.getId();
        String ticketUrl = "https://linear.app/issue/" + ticketId;

        result.put("ticketId", ticketId);
        result.put("ticketUrl", ticketUrl);
        result.put("toolName", "Linear");
        return result;
    }

    private Map<String, String> createClickupTicket(Reclamation rec) {
        Map<String, String> result = new HashMap<>();

        if (clickupApiKey == null || clickupApiKey.isEmpty()) {
            result.put("error", "ClickUp non configuré. Vérifiez clickup.api.key dans application.properties");
            return result;
        }

        String ticketId = "CUP-" + rec.getId();
        String ticketUrl = "https://app.clickup.com/task/" + ticketId;

        result.put("ticketId", ticketId);
        result.put("ticketUrl", ticketUrl);
        result.put("toolName", "ClickUp");
        return result;
    }
    @PostMapping("/reclamations/{id}/create-ticket-v2")
    public ResponseEntity<?> createTicketV2(
            @PathVariable Long id,
            @RequestBody Map<String, String> body
    ) {
        try {
            String tool = body.get("tool");

            Reclamation rec = reclamationRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Réclamation non trouvée"));

            TicketResponseDTO dto = ticketService.createTicket(rec, tool);

            return ResponseEntity.ok(Map.of(
                    "ticketId", dto.getTicketId(),
                    "ticketUrl", dto.getTicketUrl(),
                    "toolName", dto.getToolName(),
                    "alreadyExists", false
            ));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }


}