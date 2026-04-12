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
    public ResponseEntity<?> sendMessage(@RequestBody ChatMessage chatMessage) {
        log.info("========== SEND CHAT MESSAGE REQUEST ==========");
        
        try {
            if (chatMessage.getSessionId() == null) {
                return ResponseEntity.badRequest().body(new ErrorResponse("Session ID is required", 400));
            }

            if (chatMessage.getSenderId() == null || chatMessage.getSenderId().isBlank()) {
                return ResponseEntity.badRequest().body(new ErrorResponse("Sender ID is required", 400));
            }

            if (chatMessage.getContent() == null || chatMessage.getContent().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(new ErrorResponse("Message content cannot be empty", 400));
            }

            String senderId = chatMessage.getSenderId().trim();
            log.info("Sender ID: {}", senderId);

            return liveSessionRepository.findById(chatMessage.getSessionId()).map(session -> {
                if (!Boolean.TRUE.equals(session.getIsActive())) {
                    return ResponseEntity.status(403).body(new ErrorResponse("This live session is not active", 403));
                }

                Boolean isChatEnabled = session.getChatEnabled() == null ? true : session.getChatEnabled();
                if (!isChatEnabled) {
                    return ResponseEntity.status(403).body(new ErrorResponse("Chat is disabled for this session", 403));
                }

                boolean senderIsTrainer = sessionAccessService.isCourseTrainer(session.getCourseId(), senderId);
                boolean senderIsPaidLearner = sessionAccessService.hasPaidEnrollment(session.getCourseId(), senderId);

                if (!senderIsTrainer && !senderIsPaidLearner) {
                    return ResponseEntity.status(403).body(new ErrorResponse("Only the trainer and learners who paid for this course can send chat messages", 403));
                }

                // Apply spam protection only to learners (not trainers)
                if (!senderIsTrainer) {
                    ResponseEntity<?> spamCheckResult = checkSpamProtection(senderId);
                    if (spamCheckResult != null) {
                        return spamCheckResult;
                    }
                }

                chatMessage.setSenderId(senderId);
                chatMessage.setContent(chatMessage.getContent().trim());
                chatMessage.setTimestamp(LocalDateTime.now());
                
                ChatMessage savedMessage = chatMessageRepository.save(chatMessage);
                log.info("Message sent successfully by user {}", senderId);
                return ResponseEntity.ok((Object) savedMessage);
            }).orElse(ResponseEntity.badRequest().body(new ErrorResponse("Session not found", 400)));
            
        } catch (Exception e) {
            log.error("Error sending message", e);
            return ResponseEntity.status(500).body(new ErrorResponse("Internal server error: " + e.getMessage(), 500));
        }
    }

    private ResponseEntity<?> checkSpamProtection(String userId) {
        LocalDateTime now = LocalDateTime.now();

        // Check if user is in timeout
        LocalDateTime timeoutUntil = userTimeouts.get(userId);
        if (timeoutUntil != null && now.isBefore(timeoutUntil)) {
            long remainingSeconds = java.time.Duration.between(now, timeoutUntil).getSeconds();
            return ResponseEntity.status(429).body("You are in timeout for " + remainingSeconds + " seconds due to spam. Please wait.");
        }

        // Check throttle (2 seconds between messages)
        LocalDateTime lastMessage = lastMessageTime.get(userId);
        if (lastMessage != null) {
            long secondsSinceLastMessage = java.time.Duration.between(lastMessage, now).getSeconds();
            if (secondsSinceLastMessage < THROTTLE_SECONDS) {
                return ResponseEntity.status(429).body("Please wait " + (THROTTLE_SECONDS - secondsSinceLastMessage) + " seconds before sending another message.");
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
            return ResponseEntity.status(429).body("TIMEOUT: You sent too many messages too quickly. You are blocked for 1 minute.");
        }

        // Record this message timestamp
        timestamps.add(now);
        lastMessageTime.put(userId, now);
        return null; // No spam detected
    }

    @GetMapping("/session/{sessionId}")
    public ResponseEntity<?> getMessages(
            @PathVariable Long sessionId,
            @RequestParam(required = false) String userId
    ) {
        LiveSession session = liveSessionRepository.findById(sessionId).orElse(null);
        if (session == null) {
            return ResponseEntity.badRequest().body("Session not found.");
        }

        if (userId != null && !userId.isBlank()) {
            String candidateUserId = userId.trim();
            boolean isTrainer = sessionAccessService.isCourseTrainer(session.getCourseId(), candidateUserId);
            boolean isPaidLearner = sessionAccessService.hasPaidEnrollment(session.getCourseId(), candidateUserId);

            if (!isTrainer && !isPaidLearner) {
                return ResponseEntity.status(403).body("Only the trainer and learners who paid for this course can access chat messages.");
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
    public ResponseEntity<?> deleteMessage(@PathVariable Long messageId, @RequestParam String userId) {
        return chatMessageRepository.findById(messageId).map(message -> {
            LiveSession session = liveSessionRepository.findById(message.getSessionId()).orElse(null);
            if (session == null) {
                return ResponseEntity.status(400).body("Session unavailable");
            }

            if (!sessionAccessService.isCourseTrainer(session.getCourseId(), userId)) {
                return ResponseEntity.status(403).body("Only the course trainer can delete messages.");
            }

            chatMessageRepository.delete(message);
            return ResponseEntity.ok().body("Message deleted.");
        }).orElse(ResponseEntity.notFound().build());
    }
}
