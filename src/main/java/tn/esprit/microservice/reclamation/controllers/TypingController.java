package tn.esprit.microservice.reclamation.controllers;


import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Gère l'indicateur "en train d'écrire" en temps réel via WebSocket STOMP.
 *
 * Flux :
 *  Admin tape  → POST /app/typing/{reclamationId}  { sender: "ADMIN", isTyping: true  }
 *  Admin arrête → POST /app/typing/{reclamationId}  { sender: "ADMIN", isTyping: false }
 *  Serveur broadcast → /topic/typing/{reclamationId} { sender, isTyping }
 *  Angular apprenant écoute /topic/typing/{id} et affiche les points
 *
 * Auto-stop : si l'admin ne renvoie pas d'événement pendant 4s → isTyping=false automatique
 */
@Controller
public class
TypingController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    // Map reclamationId → ScheduledFuture (timer auto-stop)
    private final Map<Long, ScheduledFuture<?>> autoStopTimers = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);

    // ── DTO ──────────────────────────────────────────────────────────────────────
    public static class TypingEvent {
        public String sender;   // "ADMIN" ou "LEARNER"
        public boolean isTyping;
        public Long reclamationId;

        public TypingEvent() {}
        public TypingEvent(String sender, boolean isTyping, Long reclamationId) {
            this.sender = sender;
            this.isTyping = isTyping;
            this.reclamationId = reclamationId;
        }
    }

    /**
     * Reçoit un événement de frappe via STOMP.
     * Le client envoie vers /app/typing/{reclamationId}
     * Le serveur broadcast vers /topic/typing/{reclamationId}
     */
    @MessageMapping("/typing/{reclamationId}")
    public void handleTyping(
            @org.springframework.messaging.handler.annotation.DestinationVariable Long reclamationId,
            TypingEvent event) {

        event.reclamationId = reclamationId;

        // Broadcast à tous les abonnés de ce topic
        messagingTemplate.convertAndSend(
                "/topic/typing/" + reclamationId,
                event
        );

        // Auto-stop : annuler le timer précédent si existant
        ScheduledFuture<?> existing = autoStopTimers.get(reclamationId);
        if (existing != null) existing.cancel(false);

        if (event.isTyping) {
            // Programmer un arrêt automatique après 4 secondes sans activité
            ScheduledFuture<?> timer = scheduler.schedule(() -> {
                TypingEvent stopEvent = new TypingEvent(event.sender, false, reclamationId);
                messagingTemplate.convertAndSend("/topic/typing/" + reclamationId, stopEvent);
                autoStopTimers.remove(reclamationId);
            }, 4, TimeUnit.SECONDS);

            autoStopTimers.put(reclamationId, timer);
        } else {
            autoStopTimers.remove(reclamationId);
        }
    }

    /**
     * Endpoint REST optionnel pour les clients qui ne supportent pas STOMP.
     * Utilisé par l'admin via HTTP POST si nécessaire.
     */
    @RestController
    @RequestMapping("/msreclamation")
    public static class TypingRestController {

        @Autowired
        private SimpMessagingTemplate messagingTemplate;

        @PostMapping("/reclamations/{id}/typing")
        public ResponseEntity<?> notifyTyping(
                @PathVariable Long id,
                @RequestBody Map<String, Object> payload) {

            String sender = payload.getOrDefault("sender", "ADMIN").toString();
            boolean isTyping = Boolean.parseBoolean(
                    payload.getOrDefault("isTyping", "false").toString()
            );

            TypingEvent event = new TypingEvent(sender, isTyping, id);
            messagingTemplate.convertAndSend("/topic/typing/" + id, event);

            return ResponseEntity.ok(Map.of("sent", true));
        }
    }
}