package tn.esprit.microservice.reclamation.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import tn.esprit.microservice.reclamation.entities.Reclamation;
import tn.esprit.microservice.reclamation.entities.ReclamationStatus;
import tn.esprit.microservice.reclamation.services.interfaces.IReclamationService;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/msreclamation/ai")
//@CrossOrigin(origins = "http://localhost:4200")
public class AiSuggestionController {

    @Autowired
    private IReclamationService reclamationService;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${anthropic.api.key}")
    private String apiKey;
    @Value("${anthropic.api.key2}")   // ← 2ème clé
    private String apiKey2;

    // ── Endpoint principal ────────────────────────────────────────────────────
    @PostMapping("/suggest/{reclamationId}")
    public ResponseEntity<?> suggestResponse(@PathVariable Long reclamationId) {
        try {
            Reclamation current = reclamationService.getReclamationById(reclamationId);

            // Essayer Gemini d'abord
            try {
                List<Reclamation> resolved = reclamationService
                        .getReclamationsByStatus(ReclamationStatus.RESOLVED)
                        .stream()
                        .filter(r -> r.getType() == current.getType())
                        .limit(5)
                        .collect(Collectors.toList());

                String geminiUrl = "https://generativelanguage.googleapis.com/v1beta/models/"
                        + "gemini-1.5-flash:generateContent?key=" + apiKey;

                Map<String, Object> body = Map.of(
                        "contents", List.of(Map.of("parts", List.of(Map.of("text",
                                buildPrompt(current, buildContext(resolved))))))
                );

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);

                ResponseEntity<Map> response = restTemplate.exchange(
                        geminiUrl, HttpMethod.POST,
                        new HttpEntity<>(body, headers), Map.class
                );

                System.out.println("✅ Gemini OK");
                String raw = extractGeminiText(response.getBody());
                List<String> suggestions = parseSuggestions(raw);
                return ResponseEntity.ok(Map.of("suggestions", suggestions));

            } catch (Exception geminiError) {
                // Gemini indisponible → fallback local silencieux
                System.out.println("⚠️ Gemini indisponible, fallback local: " + geminiError.getMessage());
                return ResponseEntity.ok(Map.of("suggestions", generateLocalSuggestions(current)));
            }

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
    // ── Construire le contexte depuis les réclamations résolues ───────────────
    private List<String> generateLocalSuggestions(Reclamation rec) {
        String type   = rec.getType() != null ? rec.getType().toString() : "OTHER";
        String amount = rec.getAmount() != null ? rec.getAmount() + " DT" : "ce montant";

        java.util.Map<String, List<String>> templates = new java.util.HashMap<>();

        templates.put("PAYMENT", List.of(
                "Bonjour, nous avons bien reçu votre réclamation concernant le prélèvement de "
                        + amount + ". Notre équipe financière vérifie votre transaction et vous remboursera sous 3 à 5 jours ouvrables.",
                "Merci pour votre signalement. Un avoir a été initié pour le montant de "
                        + amount + ". Il apparaîtra sur votre prochain relevé bancaire sous 48h.",
                "Nous nous excusons pour ce désagrément. Le remboursement de "
                        + amount + " sera crédité sur votre compte. Vous recevrez une confirmation par email."
        ));

        templates.put("TECHNICAL", List.of(
                "Bonjour, nous avons bien reçu votre signalement technique. Notre équipe analyse le problème et reviendra vers vous sous 24h.",
                "Merci pour votre rapport. Pouvez-vous vider le cache de votre navigateur et réessayer ? Si le problème persiste, nous escaladons le ticket.",
                "Nous avons identifié le problème technique signalé. Un correctif sera déployé sous 48h. Nous vous notifierons dès sa mise en ligne."
        ));

        templates.put("CONTENT", List.of(
                "Bonjour, merci de nous avoir signalé ce problème de contenu. Nous vérifions le cours concerné et corrigerons l'erreur sous 24h.",
                "Nous avons transmis votre signalement à l'équipe pédagogique. Le contenu sera mis à jour très prochainement.",
                "Le problème sur ce module a été identifié et sera corrigé dans la prochaine mise à jour du cours."
        ));

        templates.put("ACCESS", List.of(
                "Bonjour, nous avons réinitialisé vos droits d'accès. Veuillez vous déconnecter et vous reconnecter pour accéder au cours.",
                "Votre accès a été vérifié et restauré manuellement. Si le problème persiste, effacez les cookies de votre navigateur.",
                "Nous avons détecté une anomalie sur vos droits d'accès et l'avons corrigée. Accès au cours rétabli."
        ));

        templates.put("CERTIFICATE", List.of(
                "Bonjour, nous vérifions l'état de votre certificat. Il sera disponible dans votre espace personnel sous 24h.",
                "Votre certificat de complétion a été régénéré manuellement et est maintenant disponible dans votre espace apprenant.",
                "Nous avons constaté un délai dans l'émission de votre certificat. Il sera disponible demain matin avec confirmation par email."
        ));

        templates.put("OTHER", List.of(
                "Bonjour, nous avons bien reçu votre réclamation et la traitons en priorité. Notre équipe vous contactera sous 24h.",
                "Merci de nous avoir contactés. Pouvez-vous nous donner plus de détails afin que nous puissions vous aider efficacement ?",
                "Votre demande a été transmise au service concerné. Nous reviendrons vers vous avec une réponse complète sous 48h."
        ));

        return templates.getOrDefault(type, templates.get("OTHER"));
    }
    // Ajoutez cette méthode après generateLocalSuggestions()
    private String buildContext(List<Reclamation> resolved) {
        if (resolved.isEmpty()) return "Aucune réclamation similaire résolue.";

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < resolved.size(); i++) {
            Reclamation r = resolved.get(i);
            sb.append(String.format(
                    "Exemple %d:\nSujet: %s\nDescription: %s\n\n",
                    i + 1,
                    r.getSubject() != null ? r.getSubject() : "",
                    r.getDescription() != null ? r.getDescription() : ""
            ));
        }
        return sb.toString();
    }

    // ── Construire le prompt envoyé à Gemini ──────────────────────────────────
    private String buildPrompt(Reclamation current, String context) {
        return String.format("""
            Tu es un assistant support pour une plateforme e-learning tunisienne.

            Réclamation actuelle (type: %s, priorité: %d):
            Sujet: %s
            Description: %s

            Réclamations similaires résolues dans le passé:
            %s

            Génère exactement 3 suggestions de réponses courtes et professionnelles
            en français pour cette réclamation.

            Format STRICT — retourne UNIQUEMENT ce JSON, sans texte avant ni après,
            sans balises markdown, sans backticks:
            {"suggestions":["réponse 1","réponse 2","réponse 3"]}
            """,
                current.getType() != null ? current.getType().toString() : "OTHER",
                current.getPriority() != null ? current.getPriority() : 3,
                current.getSubject() != null ? current.getSubject() : "",
                current.getDescription() != null ? current.getDescription() : "",
                context
        );
    }

    // ── Extraire le texte brut de la réponse Gemini ───────────────────────────
    @SuppressWarnings("unchecked")
    private String extractGeminiText(Map body) {
        List<Map> candidates = (List<Map>) body.get("candidates");
        Map content = (Map) candidates.get(0).get("content");
        List<Map> parts = (List<Map>) content.get("parts");
        return (String) parts.get(0).get("text");
    }

    // ── Parser le JSON retourné par Gemini ────────────────────────────────────
    @SuppressWarnings("unchecked")
    private List<String> parseSuggestions(String raw) {
        try {
            // Nettoyer les backticks et balises markdown éventuels
            String clean = raw
                    .replaceAll("```json", "")
                    .replaceAll("```", "")
                    .trim();

            // Extraire uniquement la partie JSON si du texte parasite précède
            int start = clean.indexOf('{');
            int end   = clean.lastIndexOf('}');
            if (start != -1 && end != -1) {
                clean = clean.substring(start, end + 1);
            }

            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> parsed = mapper.readValue(clean, Map.class);
            return (List<String>) parsed.get("suggestions");

        } catch (Exception e) {
            // Fallback : retourner le texte brut comme unique suggestion
            System.err.println("Erreur parsing suggestions: " + e.getMessage());
            System.err.println("Raw response: " + raw);
            return List.of(
                    "Bonjour, nous avons bien reçu votre réclamation et la traitons en priorité.",
                    "Merci de nous avoir contactés. Notre équipe reviendra vers vous sous 24h.",
                    "Votre demande a été transmise au service concerné. Nous vous répondrons rapidement."
            );
        }
    }
}