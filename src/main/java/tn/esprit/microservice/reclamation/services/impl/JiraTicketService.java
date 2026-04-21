package tn.esprit.microservice.reclamation.services.impl;

// JiraTicketService.java

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import tn.esprit.microservice.reclamation.DTO.TicketResponseDTO;
import tn.esprit.microservice.reclamation.entities.Reclamation;
import tn.esprit.microservice.reclamation.services.interfaces.TicketService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class JiraTicketService implements TicketService {

    @Value("${jira.url}")
    private String jiraUrl;

    @Value("${jira.email}")
    private String jiraEmail;

    @Value("${jira.api.token}")
    private String jiraToken;

    @Value("${jira.project.key}")
    private String projectKey;

    @Value("${jira.issue.type}")
    private String issueType;

    private final RestTemplate restTemplate;

    public JiraTicketService() {
        this.restTemplate = new RestTemplate();
    }

    @Override
    public TicketResponseDTO createTicket(Reclamation reclamation, String tool) {

        Map<String, Object> payload = buildJiraPayload(reclamation);
        HttpHeaders headers = buildJiraHeaders();

        String url = jiraUrl + "/rest/api/3/issue";

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new RuntimeException("Jira API failed: " + response.getStatusCode());
            }

            Map responseBody = response.getBody();

            if (!responseBody.containsKey("key")) {
                throw new RuntimeException("Jira response invalid: " + responseBody);
            }

            String ticketId = responseBody.get("key").toString();
            String ticketUrl = jiraUrl + "/browse/" + ticketId;

            return new TicketResponseDTO(ticketId, ticketUrl, "Jira", false);

        } catch (Exception e) {
            throw new RuntimeException("Erreur Jira API: " + e.getMessage(), e);
        }
    }
    private HttpHeaders buildJiraHeaders() {
        String auth = jiraEmail + ":" + jiraToken;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON)); // ✅ IMPORTANT
        headers.set("Authorization", "Basic " + encodedAuth);

        return headers;
    }

    private Map<String, Object> buildJiraPayload(Reclamation rec) {

        return Map.of(
                "fields", Map.of(
                        "project", Map.of("key", projectKey),

                        "summary", "[Réclamation #" + rec.getId() + "] " + truncate(rec.getSubject(), 200),

                        "description", Map.of(
                                "type", "doc",
                                "version", 1,
                                "content", List.of(
                                        Map.of(
                                                "type", "paragraph",
                                                "content", List.of(
                                                        Map.of(
                                                                "type", "text",
                                                                "text", buildPlainDescription(rec)
                                                        )
                                                )
                                        )
                                )
                        ),

                        "issuetype", Map.of("name", issueType),

                        "priority", Map.of("name", mapPriority(rec.getPriority())),

                        "labels", List.of("reclamation", "support")
                )
        );
    }
    private String buildPlainDescription(Reclamation rec) {
        return String.format(
                "Réclamation #%d | Apprenant: %s | Type: %s | Priorité: %s\n\n" +
                        //                          ↑ %s au lieu de %d
                        "Sujet: %s\n\n" +
                        "Description: %s\n\n" +
                        "Créé automatiquement depuis Formini Support.",
                rec.getId(),
                rec.getLearnerId(),   // ← String UUID maintenant
                rec.getType() != null ? rec.getType().toString() : "Non spécifié",
                mapPriorityLabel(rec.getPriority()),
                rec.getSubject() != null ? rec.getSubject() : "",
                rec.getDescription() != null ? rec.getDescription() : ""
        );
    }

    private String buildDescription(Reclamation rec) {
        return String.format(
                "h2. 📋 Réclamation soumise par l'apprenant\n\n" +
                        "|| Champ || Valeur ||\n" +
                        "| ID | #%d |\n" +
                        "| Apprenant | ID: %s |\n" +
                        //                  ↑ %s au lieu de %d
                        "| Type | %s |\n" +
                        "| Priorité | %s |\n" +
                        "| Statut actuel | %s |\n" +
                        "| Date de création | %s |\n\n" +
                        "h2. 📝 Sujet\n\n%s\n\n" +
                        "h2. 📄 Description\n\n%s\n\n" +
                        "h2. 🔗 Informations complémentaires\n\n%s\n\n" +
                        "---\n_Créé automatiquement depuis l'interface admin Support le %s_",
                rec.getId(),
                rec.getLearnerId(),   // ← String UUID
                rec.getType() != null ? rec.getType().toString() : "Non spécifié",
                mapPriorityLabel(rec.getPriority()),
                rec.getStatus() != null ? rec.getStatus().toString() : "PENDING",
                rec.getCreatedDate() != null ? rec.getCreatedDate().toString() : "Non définie",
                rec.getSubject() != null ? rec.getSubject() : "Non spécifié",
                rec.getDescription() != null ? rec.getDescription() : "Non spécifié",
                buildAdditionalInfo(rec),
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
        );
    }
    private String buildAdditionalInfo(Reclamation rec) {
        StringBuilder sb = new StringBuilder();

        if (rec.getCourseId() != null) {
            sb.append("* Cours concerné:* #").append(rec.getCourseId()).append("\n");
        }
        if (rec.getErrorCode() != null) {
            sb.append("* Code erreur:* ").append(rec.getErrorCode()).append("\n");
        }
        if (rec.getTransactionId() != null) {
            sb.append("* Transaction:* ").append(rec.getTransactionId()).append("\n");
        }
        if (rec.getAmount() != null) {
            sb.append("* Montant:* ").append(rec.getAmount()).append(" DT\n");
        }
        if (rec.getAttachmentUrl() != null) {
            sb.append("* Pièce jointe:* ").append(rec.getAttachmentUrl()).append("\n");
        }

        return sb.length() > 0 ? sb.toString() : "Aucune information supplémentaire";
    }

    private String mapPriority(Integer priority) {
        if (priority == null) return "Medium";
        return switch (priority) {
            case 1 -> "Highest";
            case 2 -> "High";
            case 3 -> "Medium";
            default -> "Low";
        };
    }

    private String mapPriorityLabel(Integer priority) {
        if (priority == null) return "Moyenne";
        return switch (priority) {
            case 1 -> "Haute 🔴";
            case 2 -> "Moyenne 🟡";
            case 3 -> "Basse 🟢";
            default -> "Non définie";
        };
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        return text.length() <= maxLength ? text : text.substring(0, maxLength - 3) + "...";
    }
}