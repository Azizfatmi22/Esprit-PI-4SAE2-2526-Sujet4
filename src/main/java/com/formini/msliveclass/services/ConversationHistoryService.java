package com.formini.msliveclass.services;

import com.formini.msliveclass.dto.ConversationMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages conversation history for each learner
 * Stores recent messages to provide context for the AI
 */
@Service
public class ConversationHistoryService {

    private static final Logger logger = LoggerFactory.getLogger(ConversationHistoryService.class);
    
    // Store conversation history per learner (learnerId -> messages)
    private final Map<String, List<ConversationMessage>> conversationHistory = new ConcurrentHashMap<>();
    
    // Maximum messages to keep per conversation (to prevent memory issues)
    private static final int MAX_HISTORY_SIZE = 20;
    
    // Session timeout in minutes
    private static final int SESSION_TIMEOUT_MINUTES = 30;

    /**
     * Add a message to the conversation history
     */
    public void addMessage(String learnerId, String role, String content) {
        if (learnerId == null || learnerId.trim().isEmpty()) {
            return;
        }

        List<ConversationMessage> history = conversationHistory.computeIfAbsent(
            learnerId, 
            k -> new ArrayList<>()
        );

        // Clean old messages if session timed out
        cleanOldMessages(history);

        // Add new message
        history.add(new ConversationMessage(role, content));

        // Keep only recent messages
        if (history.size() > MAX_HISTORY_SIZE) {
            history.remove(0);
        }

        logger.debug("📝 Added {} message for learner {}. History size: {}", 
            role, learnerId, history.size());
    }

    /**
     * Get conversation history for a learner
     */
    public List<ConversationMessage> getHistory(String learnerId) {
        if (learnerId == null || learnerId.trim().isEmpty()) {
            return new ArrayList<>();
        }

        List<ConversationMessage> history = conversationHistory.get(learnerId);
        if (history == null) {
            return new ArrayList<>();
        }

        // Clean old messages
        cleanOldMessages(history);

        return new ArrayList<>(history);
    }

    /**
     * Clear conversation history for a learner
     */
    public void clearHistory(String learnerId) {
        if (learnerId != null && !learnerId.trim().isEmpty()) {
            conversationHistory.remove(learnerId);
            logger.info("🗑️ Cleared conversation history for learner {}", learnerId);
        }
    }

    /**
     * Get formatted conversation context for the AI
     */
    public String getFormattedContext(String learnerId) {
        List<ConversationMessage> history = getHistory(learnerId);
        
        if (history.isEmpty()) {
            return "";
        }

        StringBuilder context = new StringBuilder();
        context.append("CONVERSATION HISTORY:\n");
        
        for (ConversationMessage msg : history) {
            String roleLabel = "user".equals(msg.getRole()) ? "User" : "Assistant";
            context.append(roleLabel).append(": ").append(msg.getContent()).append("\n");
        }
        
        context.append("\n");
        return context.toString();
    }

    /**
     * Check if this is a new conversation (no recent history)
     */
    public boolean isNewConversation(String learnerId) {
        List<ConversationMessage> history = getHistory(learnerId);
        return history.isEmpty();
    }

    /**
     * Get the last user message
     */
    public String getLastUserMessage(String learnerId) {
        List<ConversationMessage> history = getHistory(learnerId);
        
        for (int i = history.size() - 1; i >= 0; i--) {
            ConversationMessage msg = history.get(i);
            if ("user".equals(msg.getRole())) {
                return msg.getContent();
            }
        }
        
        return null;
    }

    /**
     * Clean messages older than session timeout
     */
    private void cleanOldMessages(List<ConversationMessage> history) {
        if (history.isEmpty()) {
            return;
        }

        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(SESSION_TIMEOUT_MINUTES);
        history.removeIf(msg -> msg.getTimestamp().isBefore(cutoff));
    }

    /**
     * Get conversation statistics
     */
    public int getActiveConversations() {
        return conversationHistory.size();
    }
}
