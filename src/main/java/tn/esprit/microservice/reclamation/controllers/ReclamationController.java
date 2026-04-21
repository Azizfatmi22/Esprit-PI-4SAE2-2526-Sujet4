package tn.esprit.microservice.reclamation.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.microservice.reclamation.entities.Reclamation;
import tn.esprit.microservice.reclamation.entities.ReclamationResponse;
import tn.esprit.microservice.reclamation.entities.ReclamationStatus;
import tn.esprit.microservice.reclamation.entities.ReclamationType;
import tn.esprit.microservice.reclamation.repositories.ReclamationRepository;
import tn.esprit.microservice.reclamation.services.impl.AutoCategorizationService;
import tn.esprit.microservice.reclamation.services.impl.ExtractionService;
import tn.esprit.microservice.reclamation.services.interfaces.IReclamationService;
import tn.esprit.microservice.reclamation.services.interfaces.IReclamationResponseService;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/msreclamation")
//@CrossOrigin("http://localhost:4200")
public class ReclamationController {

    @Autowired
    private IReclamationService reclamationService;

    @Autowired
    private IReclamationResponseService responseService;

    // Dans ReclamationController.java — ajouter :
    @Autowired
    private ReclamationRepository reclamationRepository;

    @Autowired
    private AutoCategorizationService autoCategorizationService;

    @Autowired
    private ExtractionService extractionService;

    // Endpoint de test pour vérifier que le service fonctionne
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "Reclamation-service");
        response.put("message", "Service is running");
        return ResponseEntity.ok(response);
    }

    // Créer une nouvelle réclamation
    @PostMapping("/reclamations")
    public ResponseEntity<?> createReclamation(@RequestBody Reclamation reclamation) {
        try {
            Reclamation created = reclamationService.createReclamation(reclamation);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de la création de la réclamation: " + e.getMessage());
        }
    }

    // Récupérer toutes les réclamations
    @GetMapping("/reclamations")
    public ResponseEntity<List<Reclamation>> getAllReclamations() {
        return ResponseEntity.ok(reclamationService.getAllReclamations());
    }

    // Récupérer une réclamation par ID
    @GetMapping("/reclamations/{id}")
    public ResponseEntity<?> getReclamationById(@PathVariable Long id) {
        try {
            Reclamation reclamation = reclamationService.getReclamationById(id);
            return ResponseEntity.ok(reclamation);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    // Récupérer les réclamations d'un apprenant
    @GetMapping("/reclamations/learner/{learnerId}")
    public ResponseEntity<List<Reclamation>> getReclamationsByLearner(@PathVariable String learnerId) {
        return ResponseEntity.ok(reclamationService.getReclamationsByLearner(learnerId));
    }

    // Récupérer les réclamations par statut
    @GetMapping("/reclamations/status/{status}")
    public ResponseEntity<List<Reclamation>> getReclamationsByStatus(@PathVariable String status) {
        try {
            ReclamationStatus reclamationStatus = ReclamationStatus.valueOf(status.toUpperCase());
            return ResponseEntity.ok(reclamationService.getReclamationsByStatus(reclamationStatus));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // Récupérer les réclamations par type
    @GetMapping("/reclamations/type/{type}")
    public ResponseEntity<List<Reclamation>> getReclamationsByType(@PathVariable String type) {
        try {
            ReclamationType reclamationType = ReclamationType.valueOf(type.toUpperCase());
            return ResponseEntity.ok(reclamationService.getReclamationsByType(reclamationType));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // Récupérer les réclamations non résolues
    @GetMapping("/reclamations/unresolved")
    public ResponseEntity<List<Reclamation>> getUnresolvedReclamations() {
        return ResponseEntity.ok(reclamationService.getUnresolvedReclamations());
    }

    // Mettre à jour une réclamation
    @PutMapping("/reclamations/{id}")
    public ResponseEntity<?> updateReclamation(@PathVariable Long id, @RequestBody Reclamation reclamation) {
        try {
            Reclamation updated = reclamationService.updateReclamation(id, reclamation);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de la mise à jour: " + e.getMessage());
        }
    }

    // Mettre à jour le statut d'une réclamation (pour les admins)
    @PutMapping("/reclamations/{id}/status")
    public ResponseEntity<?> updateReclamationStatus(
            @PathVariable Long id,
            @RequestBody Map<String, Object> payload) {
        try {
            String statusStr = payload.get("status").toString().toUpperCase();
            ReclamationStatus status = ReclamationStatus.valueOf(statusStr);
            Long adminId = payload.containsKey("adminId") ? 
                    Long.valueOf(payload.get("adminId").toString()) : null;

            Reclamation updated = reclamationService.updateReclamationStatus(id, status, adminId);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Statut invalide");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de la mise à jour du statut: " + e.getMessage());
        }
    }

    // --- GESTION DES RÉPONSES ---

    // Ajouter une réponse à une réclamation
    @PostMapping("/reclamations/{id}/responses")
    public ResponseEntity<?> addResponse(
            @PathVariable Long id,
            @RequestBody Map<String, Object> payload) {
        try {
            String responseText = payload.get("responseText").toString();

            // ← learnerId est String (UUID ou numérique)
            String learnerId = null;
            if (payload.containsKey("learnerId") && payload.get("learnerId") != null) {
                learnerId = payload.get("learnerId").toString();
            }
            // Support ancien champ adminId
            if (learnerId == null && payload.containsKey("adminId") && payload.get("adminId") != null) {
                learnerId = payload.get("adminId").toString();
            }

            Boolean isInternal = false;
            if (payload.containsKey("isInternal") && payload.get("isInternal") != null) {
                isInternal = Boolean.valueOf(payload.get("isInternal").toString());
            }

            String senderType = "ADMIN";
            if (payload.containsKey("senderType") && payload.get("senderType") != null) {
                senderType = payload.get("senderType").toString();
            }

            String quotedText = payload.containsKey("quotedText") && payload.get("quotedText") != null
                    ? payload.get("quotedText").toString() : null;
            String quotedAuthor = payload.containsKey("quotedAuthor") && payload.get("quotedAuthor") != null
                    ? payload.get("quotedAuthor").toString() : null;

            ReclamationResponse response = responseService.createResponse(id, learnerId, responseText, isInternal);

            // Mettre à jour les champs supplémentaires
            response.setSenderType(senderType);
            if (quotedText != null) response.setQuotedText(quotedText);
            if (quotedAuthor != null) response.setQuotedAuthor(quotedAuthor);
            ReclamationResponse saved = responseService.saveResponse(response);

            return ResponseEntity.status(HttpStatus.CREATED).body(saved);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur: " + e.getMessage());
        }
    }
    // Récupérer toutes les réponses d'une réclamation
    @GetMapping("/reclamations/{id}/responses")
    public ResponseEntity<List<ReclamationResponse>> getReclamationResponses(@PathVariable Long id) {
        return ResponseEntity.ok(responseService.getResponsesByReclamationId(id));
    }

    // Récupérer les réponses publiques d'une réclamation (pour l'apprenant)
    @GetMapping("/reclamations/{id}/responses/public")
    public ResponseEntity<List<ReclamationResponse>> getPublicResponses(@PathVariable Long id) {
        return ResponseEntity.ok(responseService.getPublicResponsesByReclamationId(id));
    }

    // Récupérer les commentaires internes d'une réclamation (pour les admins)
    @GetMapping("/reclamations/{id}/responses/internal")
    public ResponseEntity<List<ReclamationResponse>> getInternalComments(@PathVariable Long id) {
        return ResponseEntity.ok(responseService.getInternalCommentsByReclamationId(id));
    }

    // Mettre à jour une réponse
    @PutMapping("/responses/{id}")
    public ResponseEntity<?> updateResponse(
            @PathVariable Long id,
            @RequestBody Map<String, Object> payload) {
        try {
            String responseText = payload.get("responseText").toString();
            ReclamationResponse updated = responseService.updateResponse(id, responseText);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de la mise à jour: " + e.getMessage());
        }
    }

    // Supprimer une réponse
    @DeleteMapping("/responses/{id}")
    public ResponseEntity<?> deleteResponse(@PathVariable Long id) {
        try {
            responseService.deleteResponse(id);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Réponse supprimée avec succès");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de la suppression: " + e.getMessage());
        }
    }

    // Supprimer une réclamation
    @DeleteMapping("/reclamations/{id}")
    public ResponseEntity<?> deleteReclamation(@PathVariable Long id) {
        try {
            reclamationService.deleteReclamation(id);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Réclamation supprimée avec succès");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de la suppression: " + e.getMessage());
        }
    }

    // Statistiques
    @GetMapping("/reclamations/stats")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("total", reclamationService.getAllReclamations().size());
        stats.put("pending", reclamationService.getReclamationsByStatus(ReclamationStatus.PENDING).size());
        stats.put("inProgress", reclamationService.getReclamationsByStatus(ReclamationStatus.IN_PROGRESS).size());
        stats.put("resolved", reclamationService.getReclamationsByStatus(ReclamationStatus.RESOLVED).size());
        stats.put("closed", reclamationService.getReclamationsByStatus(ReclamationStatus.CLOSED).size());
        stats.put("rejected", reclamationService.getReclamationsByStatus(ReclamationStatus.REJECTED).size());
        stats.put("unresolved", reclamationService.getUnresolvedReclamations().size());
        
        return ResponseEntity.ok(stats);
    }
    @PostMapping("/reclamations/{id}/satisfaction")
    public ResponseEntity<?> submitSatisfaction(
            @PathVariable Long id,
            @RequestBody Map<String, Object> payload) {
        try {
            Reclamation reclamation = reclamationService.getReclamationById(id);

            Integer score = Integer.valueOf(payload.get("score").toString());
            String comment = payload.containsKey("comment") && payload.get("comment") != null
                    ? payload.get("comment").toString() : null;

            reclamation.setSatisfactionScore(score);
            reclamation.setSatisfactionComment(comment);
            reclamation.setSatisfactionDate(LocalDateTime.now());

            Reclamation updated = reclamationRepository.save(reclamation);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur: " + e.getMessage());
        }
    }
    // ── Analytics ─────────────────────────────────────────────────────────────────

    // Volume par semaine (7 dernières semaines)
    @GetMapping("/reclamations/analytics/weekly")
    public ResponseEntity<List<Map<String, Object>>> getWeeklyVolume() {
        List<Reclamation> all = reclamationService.getAllReclamations();
        List<Map<String, Object>> result = new java.util.ArrayList<>();

        for (int i = 6; i >= 0; i--) {
            java.time.LocalDateTime start = java.time.LocalDateTime.now().minusWeeks(i).with(java.time.DayOfWeek.MONDAY).toLocalDate().atStartOfDay();
            java.time.LocalDateTime end = start.plusWeeks(1);
            int week = i;
            long count = all.stream()
                    .filter(r -> r.getCreatedDate() != null && r.getCreatedDate().isAfter(start) && r.getCreatedDate().isBefore(end))
                    .count();
            Map<String, Object> entry = new java.util.HashMap<>();
            entry.put("label", "S-" + i);
            entry.put("count", count);
            result.add(entry);
        }
        return ResponseEntity.ok(result);
    }

    // Temps moyen de résolution par type (en heures)
    @GetMapping("/reclamations/analytics/resolution-time")
    public ResponseEntity<List<Map<String, Object>>> getResolutionTimeByType() {
        List<Reclamation> resolved = reclamationService.getReclamationsByStatus(ReclamationStatus.RESOLVED);
        List<Reclamation> closed   = reclamationService.getReclamationsByStatus(ReclamationStatus.CLOSED);
        List<Reclamation> all = new java.util.ArrayList<>();
        all.addAll(resolved); all.addAll(closed);

        Map<String, List<Long>> byType = new java.util.HashMap<>();
        for (Reclamation r : all) {
            if (r.getResolvedDate() != null && r.getCreatedDate() != null) {
                long hours = java.time.temporal.ChronoUnit.HOURS.between(r.getCreatedDate(), r.getResolvedDate());
                byType.computeIfAbsent(r.getType().toString(), k -> new java.util.ArrayList<>()).add(hours);
            }
        }

        List<Map<String, Object>> result = new java.util.ArrayList<>();
        byType.forEach((type, times) -> {
            Map<String, Object> entry = new java.util.HashMap<>();
            entry.put("type", type);
            entry.put("avgHours", times.stream().mapToLong(Long::longValue).average().orElse(0));
            result.add(entry);
        });
        return ResponseEntity.ok(result);
    }

    // Distribution par statut
    @GetMapping("/reclamations/analytics/by-status")
    public ResponseEntity<Map<String, Long>> getByStatus() {
        Map<String, Long> result = new java.util.LinkedHashMap<>();
        result.put("PENDING",     (long) reclamationService.getReclamationsByStatus(ReclamationStatus.PENDING).size());
        result.put("IN_PROGRESS", (long) reclamationService.getReclamationsByStatus(ReclamationStatus.IN_PROGRESS).size());
        result.put("RESOLVED",    (long) reclamationService.getReclamationsByStatus(ReclamationStatus.RESOLVED).size());
        result.put("CLOSED",      (long) reclamationService.getReclamationsByStatus(ReclamationStatus.CLOSED).size());
        result.put("REJECTED",    (long) reclamationService.getReclamationsByStatus(ReclamationStatus.REJECTED).size());
        return ResponseEntity.ok(result);
    }

    // Distribution par type
    @GetMapping("/reclamations/analytics/by-type")
    public ResponseEntity<List<Map<String, Object>>> getByType() {
        List<Reclamation> all = reclamationService.getAllReclamations();
        Map<String, Long> counts = new java.util.LinkedHashMap<>();
        for (Reclamation r : all) {
            String t = r.getType().toString();
            counts.put(t, counts.getOrDefault(t, 0L) + 1);
        }
        List<Map<String, Object>> result = new java.util.ArrayList<>();
        counts.forEach((type, count) -> {
            Map<String, Object> entry = new java.util.HashMap<>();
            entry.put("type", type); entry.put("count", count);
            result.add(entry);
        });
        return ResponseEntity.ok(result);
    }

    // Distribution par priorité
    @GetMapping("/reclamations/analytics/by-priority")
    public ResponseEntity<List<Map<String, Object>>> getByPriority() {
        List<Reclamation> all = reclamationService.getAllReclamations();
        Map<Integer, Long> counts = new java.util.TreeMap<>();
        for (Reclamation r : all) {
            int p = r.getPriority() != null ? r.getPriority() : 3;
            counts.put(p, counts.getOrDefault(p, 0L) + 1);
        }
        List<Map<String, Object>> result = new java.util.ArrayList<>();
        counts.forEach((priority, count) -> {
            Map<String, Object> entry = new java.util.HashMap<>();
            entry.put("priority", priority); entry.put("count", count);
            result.add(entry);
        });
        return ResponseEntity.ok(result);
    }

    @PostMapping("/responses/{id}/react")
    public ResponseEntity<?> addReaction(
            @PathVariable Long id,
            @RequestBody Map<String, Object> payload) {
        try {
            String emoji = payload.get("emoji").toString();
            ReclamationResponse response = responseService.getResponseById(id);
            response.setReaction(emoji);
            ReclamationResponse updated = responseService.saveResponse(response);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur: " + e.getMessage());
        }
    }

    // Version minimale qui fonctionne

    @PostMapping("/reclamations/{id}/approve")
    public ResponseEntity<?> approveReclamation(@PathVariable Long id) {
        try {
            Reclamation reclamation = reclamationService.getReclamationById(id);
            reclamation.setIsSuspect(false);
            reclamation.setModerationReason(null);
            return ResponseEntity.ok(reclamationRepository.save(reclamation));
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Erreur: " + e.getMessage());
        }
    }

    @PostMapping("/reclamations/{id}/reject")
    public ResponseEntity<?> rejectReclamation(@PathVariable Long id) {
        try {
            Reclamation reclamation = reclamationService.getReclamationById(id);
            reclamation.setIsSuspect(false);
            reclamation.setStatus(ReclamationStatus.REJECTED);
            return ResponseEntity.ok(reclamationRepository.save(reclamation));
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Erreur: " + e.getMessage());
        }
    }

    // Ajouter cet endpoint
    @PostMapping("/reclamations/detect-type")
    public ResponseEntity<?> detectReclamationType(@RequestBody Map<String, String> payload) {
        try {
            String subject = payload.getOrDefault("subject", "");
            String description = payload.getOrDefault("description", "");

            AutoCategorizationService.CategorizationResult result =
                    autoCategorizationService.detectType(subject, description);

            if (result.isHasSuggestion()) {
                return ResponseEntity.ok(Map.of(
                        "hasSuggestion", true,
                        "suggestedType", result.getSuggestedType().toString(),
                        "suggestedLabel", autoCategorizationService.getTypeLabel(result.getSuggestedType()),
                        "confidence", result.getConfidence(),
                        "matchedKeywords", result.getMatchedKeywords()
                ));
            } else {
                return ResponseEntity.ok(Map.of("hasSuggestion", false));
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/reclamations/extract")
    public ResponseEntity<?> extractInformation(@RequestBody Map<String, String> payload) {
        try {
            String text = payload.getOrDefault("text", "");
            ExtractionService.ExtractedData data = extractionService.extract(text);

            Map<String, Object> response = new HashMap<>();
            response.put("hasData", data.hasData());

            if (data.getAmount() != null) {
                response.put("amount", data.getAmount());
            }
            if (data.getTransactionId() != null) {
                response.put("transactionId", data.getTransactionId());
            }
            if (data.getErrorCode() != null) {
                response.put("errorCode", data.getErrorCode());
            }
            if (data.getEmail() != null) {
                response.put("email", data.getEmail());
            }
            if (data.getExtractedDate() != null) {
                response.put("extractedDate", data.getExtractedDate());
            }
            if (data.getInvoiceNumber() != null) {
                response.put("invoiceNumber", data.getInvoiceNumber());
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }






}
