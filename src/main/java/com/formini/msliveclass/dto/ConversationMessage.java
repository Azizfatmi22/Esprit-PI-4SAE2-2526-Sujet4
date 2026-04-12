package com.formini.msliveclass.dto;

import java.time.LocalDateTime;

/**
 * Represents a single message in a conversation
 */
public class ConversationMessage {
    private String role; // "user" or "assistant"
    private String content;
    private LocalDateTime timestamp;

    public ConversationMessage() {
    }

    public ConversationMessage(String role, String content) {
        this.role = role;
        this.content = content;
        this.timestamp = LocalDateTime.now();
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
