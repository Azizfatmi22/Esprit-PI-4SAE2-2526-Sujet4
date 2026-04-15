package com.example.msforum.controllers;

import com.example.msforum.dto.CommentRequest;
import com.example.msforum.dto.CommentResponse;
import com.example.msforum.services.CommentService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/posts/{postId}/comments")
public class CommentController {

    private final CommentService commentService;
    private final ObjectMapper objectMapper;

    public CommentController(CommentService commentService, ObjectMapper objectMapper) {
        this.commentService = commentService;
        this.objectMapper = objectMapper;
    }

    /**
     * Sample JSON:
     * {
     *   "userId": 10,
     *   "content": "Great post!"
     * }
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponse addComment(@PathVariable Long postId, @Valid @RequestBody CommentRequest request) {
        return commentService.addComment(postId, request);
    }

    @GetMapping
    public List<CommentResponse> getComments(@PathVariable Long postId, HttpServletRequest request) {
        String currentUserId = extractUserId(request);
        return commentService.getCommentsByPost(postId, currentUserId);
    }

    @PutMapping("/{commentId}/best-answer")
    public CommentResponse markBestAnswer(@PathVariable Long postId, @PathVariable Long commentId) {
        return commentService.markBestAnswer(postId, commentId);
    }

    private String extractUserId(HttpServletRequest request) {
        String token = extractBearerToken(request);
        if (token == null) {
            return null;
        }
        JsonNode payload = extractPayload(token);
        if (payload == null) {
            return null;
        }
        return payload.get("sub") != null ? payload.get("sub").asText() : null;
    }

    private String extractBearerToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || header.isBlank()) {
            return null;
        }
        if (!header.startsWith("Bearer ")) {
            return null;
        }
        return header.substring(7);
    }

    private JsonNode extractPayload(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) {
                return null;
            }
            String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            return objectMapper.readTree(payloadJson);
        } catch (Exception e) {
            return null;
        }
    }
}
