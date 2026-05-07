package com.formini.msliveclass.controllers;

import com.formini.msliveclass.dto.ErrorResponse;
import com.formini.msliveclass.entities.ChatMessage;
import com.formini.msliveclass.entities.LiveSession;
import com.formini.msliveclass.repositories.ChatMessageRepository;
import com.formini.msliveclass.repositories.LiveSessionRepository;
import com.formini.msliveclass.services.SessionAccessService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private final ChatMessageRepository chatMessageRepository;
    private final LiveSessionRepository liveSessionRepository;
    private final SessionAccessService sessionAccessService;

    // Spam protection: Track message timestamps per user
    private final Map<String, List<LocalDateTime>> userMessageTimestamps = new ConcurrentHashMap<>();
    private static final int SPAM_THRESHOLD = 4; // 4 messages
    private static final int SPAM_WINDOW_SECONDS = 10; // within 10 seconds
    private static final int TIMEOUT_DURATION_SECONDS = 60; // 1 minute timeout
    private final Map<String, LocalDateTime> userTimeouts = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> lastMessageTime = new ConcurrentHashMap<>();
    private static final int THROTTLE_SECONDS = 2; // 2 seconds between messages

    @Autowired
    public ChatController(
            ChatMessageRepository chatMessageRepository,
            LiveSessionRepository liveSessionRepository,
            SessionAccessService sessionAccessService
    ) {
        this.chatMessageRepository = chatMessageRepository;
        this.liveSessionRepository = liveSessionRepository;
        this.sessionAccessService = sessionAccessService;
    }

    @PostMapping("/send")
    public ResponseEntity<Object> sendMessage(@RequestBody ChatMessage chatMessage) {
        log.info("========== SEND CHAT MESSAGE REQUEST ==========");
        try {
            ResponseEntity<Object> validationError = validateChatMessage(chatMessage);
            if (validationError != null) return validationError;

            String senderId = chatMessage.getSenderId().trim();
            log.info("Sender ID: {}", senderId);

            return liveSessionRepository.findById(chatMessage.getSessionId())
                .map(session -> processSessionMessage(session, chatMessage, senderId))
                .orElse(ResponseEntity.badRequest().body(new ErrorResponse("Session not found", 400)));
        } catch (Exception e) {
            log.error("Error sending message", e);
            return ResponseEntity.status(500).body(new ErrorResponse("Internal server error: " + e.getMessage(), 500));
        }
    }

    private ResponseEntity<Object> validateChatMessage(ChatMessage chatMessage) {
        if (chatMessage.getSessionId() == null) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Session ID is required", 400));
        }
        if (chatMessage.getSenderId() == null || chatMessage.getSenderId().isBlank()) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Sender ID is required", 400));
        }
        if (chatMessage.getContent() == null || chatMessage.getContent().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Message content cannot be empty", 400));
        }
        return null;
    }

    private ResponseEntity<Object> processSessionMessage(LiveSession session, ChatMessage chatMessage, String senderId) {
        ResponseEntity<Object> accessError = validateSessionAccess(session, senderId);
        if (accessError != null) return accessError;

        chatMessage.setSenderId(senderId);
        chatMessage.setContent(chatMessage.getContent().trim());
        chatMessage.setTimestamp(LocalDateTime.now());

        ChatMessage savedMessage = chatMessageRepository.save(chatMessage);
        log.info("Message sent successfully by user {}", senderId);
        return ResponseEntity.ok(savedMessage);
    }

    private ResponseEntity<Object> validateSessionAccess(LiveSession session, String senderId) {
        if (!Boolean.TRUE.equals(session.getIsActive())) {
            return ResponseEntity.status(403).body(new ErrorResponse("This live session is not active", 403));
        }

        if (Boolean.FALSE.equals(session.getChatEnabled())) {
            return ResponseEntity.status(403).body(new ErrorResponse("Chat is disabled for this session", 403));
        }

        boolean isTrainer = sessionAccessService.isCourseTrainer(session.getCourseId(), senderId);
        boolean isPaidLearner = sessionAccessService.hasPaidEnrollment(session.getCourseId(), senderId);

        if (!isTrainer && !isPaidLearner) {
            return ResponseEntity.status(403).body(new ErrorResponse("Only the trainer and learners who paid for this course can send chat messages", 403));
        }

        if (!isTrainer) {
            return checkSpamProtection(senderId);
        }
        return null;
    }

    private ResponseEntity<Object> checkSpamProtection(String userId) {
        LocalDateTime now = LocalDateTime.now();

        // Check if user is in timeout
        LocalDateTime timeoutUntil = userTimeouts.get(userId);
        if (timeoutUntil != null && now.isBefore(timeoutUntil)) {
            long remainingSeconds = java.time.Duration.between(now, timeoutUntil).getSeconds();
            return ResponseEntity.status(429).body(new ErrorResponse("You are in timeout for " + remainingSeconds + " seconds due to spam. Please wait.", 429));
        }

        // Check throttle (2 seconds between messages)
        LocalDateTime lastMessage = lastMessageTime.get(userId);
        if (lastMessage != null) {
            long secondsSinceLastMessage = java.time.Duration.between(lastMessage, now).getSeconds();
            if (secondsSinceLastMessage < THROTTLE_SECONDS) {
                return ResponseEntity.status(429).body(new ErrorResponse("Please wait " + (THROTTLE_SECONDS - secondsSinceLastMessage) + " seconds before sending another message.", 429));
            }
        }

        // Get user's message history
        List<LocalDateTime> timestamps = userMessageTimestamps.computeIfAbsent(userId, k -> new java.util.ArrayList<>());

        // Clean old timestamps (older than spam window)
        timestamps.removeIf(timestamp -> 
            java.time.Duration.between(timestamp, now).getSeconds() > SPAM_WINDOW_SECONDS
        );

        // Check if user is spamming
        if (timestamps.size() >= SPAM_THRESHOLD) {
            // Apply timeout
            userTimeouts.put(userId, now.plusSeconds(TIMEOUT_DURATION_SECONDS));
            timestamps.clear();
            return ResponseEntity.status(429).body(new ErrorResponse("TIMEOUT: You sent too many messages too quickly. You are blocked for 1 minute.", 429));
        }

        // Record this message timestamp
        timestamps.add(now);
        lastMessageTime.put(userId, now);
        return null; // No spam detected
    }

    @GetMapping("/session/{sessionId}")
    public ResponseEntity<Object> getMessages(
            @PathVariable Long sessionId,
            @RequestParam(required = false) String userId
    ) {
        LiveSession session = liveSessionRepository.findById(sessionId).orElse(null);
        if (session == null) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Session not found.", 400));
        }

        if (userId != null && !userId.isBlank()) {
            String candidateUserId = userId.trim();
            boolean isTrainer = sessionAccessService.isCourseTrainer(session.getCourseId(), candidateUserId);
            boolean isPaidLearner = sessionAccessService.hasPaidEnrollment(session.getCourseId(), candidateUserId);

            if (!isTrainer && !isPaidLearner) {
                return ResponseEntity.status(403).body(new ErrorResponse("Only the trainer and learners who paid for this course can access chat messages.", 403));
            }
        }

        // Return chat history regardless of session active status
        return ResponseEntity.ok(chatMessageRepository.findBySessionId(sessionId));
    }

    @GetMapping
    public List<ChatMessage> getAllMessages() {
        return chatMessageRepository.findAll();
    }

    @DeleteMapping("/{messageId}")
    public ResponseEntity<Object> deleteMessage(@PathVariable Long messageId, @RequestParam String userId) {
        return chatMessageRepository.findById(messageId).map(message -> {
            LiveSession session = liveSessionRepository.findById(message.getSessionId()).orElse(null);
            if (session == null) {
                return ResponseEntity.status(400).body(new ErrorResponse("Session unavailable", 400));
            }

            if (!sessionAccessService.isCourseTrainer(session.getCourseId(), userId)) {
                return ResponseEntity.status(403).body(new ErrorResponse("Only the course trainer can delete messages.", 403));
            }

            chatMessageRepository.delete(message);
            return ResponseEntity.ok().body((Object) "Message deleted.");
        }).orElse(ResponseEntity.notFound().build());
    }
}
