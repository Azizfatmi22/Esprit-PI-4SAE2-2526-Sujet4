package com.example.msforum.controllers;

import com.example.msforum.dto.CommentResponse;
import com.example.msforum.dto.PostResponse;
import com.example.msforum.entities.ContentStatus;
import com.example.msforum.services.CommentService;
import com.example.msforum.services.PostService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/moderation")
public class ModerationController {

    private final PostService postService;
    private final CommentService commentService;
    private final ObjectMapper objectMapper;

    public ModerationController(PostService postService, CommentService commentService, ObjectMapper objectMapper) {
        this.postService = postService;
        this.commentService = commentService;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/posts/pending")
    public List<PostResponse> getPendingPosts(HttpServletRequest httpRequest) {
        assertAdmin(httpRequest);
        return postService.getPendingPosts();
    }

    @PutMapping("/posts/{id}/status")
    public PostResponse updatePostStatus(@PathVariable Long id, @RequestParam ContentStatus status, HttpServletRequest httpRequest) {
        assertAdmin(httpRequest);
        return postService.updatePostStatus(id, status);
    }

    @GetMapping("/comments/pending")
    public List<CommentResponse> getPendingComments(HttpServletRequest httpRequest) {
        assertAdmin(httpRequest);
        return commentService.getPendingComments();
    }

    @PutMapping("/comments/{id}/status")
    public CommentResponse updateCommentStatus(@PathVariable Long id, @RequestParam ContentStatus status, HttpServletRequest httpRequest) {
        assertAdmin(httpRequest);
        return commentService.updateCommentStatus(id, status);
    }

    private void assertAdmin(HttpServletRequest request) {
        if (!hasAdminRole(request)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only admins can perform this action");
        }
    }

    private boolean hasAdminRole(HttpServletRequest request) {
        String token = extractBearerToken(request);
        if (token == null) {
            return false;
        }
        JsonNode payload = extractPayload(token);
        if (payload == null) {
            return false;
        }
        Set<String> roles = extractRoles(payload);
        return roles.contains("ADMIN");
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

    private Set<String> extractRoles(JsonNode payload) {
        Set<String> roles = new HashSet<>();
        JsonNode realmAccess = payload.get("realm_access");
        if (realmAccess != null && realmAccess.has("roles")) {
            Iterator<JsonNode> elements = realmAccess.get("roles").elements();
            while (elements.hasNext()) {
                roles.add(elements.next().asText());
            }
        }
        return roles;
    }
}
