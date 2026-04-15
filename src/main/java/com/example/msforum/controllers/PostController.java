package com.example.msforum.controllers;

import com.example.msforum.dto.PostRequest;
import com.example.msforum.dto.PostResponse;
import com.example.msforum.entities.ContentStatus;
import com.example.msforum.entities.PostCategory;
import com.example.msforum.services.PostService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/posts")
public class PostController {

    private final PostService postService;
    private final ObjectMapper objectMapper;

    public PostController(PostService postService, ObjectMapper objectMapper) {
        this.postService = postService;
        this.objectMapper = objectMapper;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PostResponse createPost(@Valid @RequestBody PostRequest request, HttpServletRequest httpRequest) {
        assertTrainer(httpRequest);
        String authenticatedUserId = extractUserId(httpRequest);
        if (authenticatedUserId != null && !authenticatedUserId.isBlank()) {
            request.setUserId(authenticatedUserId);
        }
        return postService.createPost(request);
    }

    @GetMapping
    public List<PostResponse> getAllPosts(@RequestParam(required = false) String userId,
                                         @RequestParam(required = false) PostCategory category) {
        if (userId != null && !userId.isBlank()) {
            return postService.getPostsByUserId(userId);
        }
        return postService.getAllPosts(category);
    }

    @GetMapping("/pending")
    public List<PostResponse> getPendingPosts(HttpServletRequest httpRequest) {
        assertAdmin(httpRequest);
        return postService.getPendingPosts();
    }

    @PutMapping("/{id}/status")
    public PostResponse updatePostStatus(@PathVariable Long id, @RequestParam ContentStatus status, HttpServletRequest httpRequest) {
        assertAdmin(httpRequest);
        return postService.updatePostStatus(id, status);
    }

    @GetMapping("/{id}")
    public PostResponse getPostById(@PathVariable Long id) {
        return postService.getPostById(id);
    }

    @PutMapping("/{id}")
    public PostResponse updatePost(@PathVariable Long id, @Valid @RequestBody PostRequest request,
                                   HttpServletRequest httpRequest) {
        assertTrainer(httpRequest);
        String authenticatedUserId = extractUserId(httpRequest);
        if (authenticatedUserId != null && !authenticatedUserId.isBlank()) {
            request.setUserId(authenticatedUserId);
        }
        return postService.updatePost(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePost(@PathVariable Long id, HttpServletRequest httpRequest) {
        assertTrainer(httpRequest);
        postService.deletePost(id);
    }

    private void assertTrainer(HttpServletRequest request) {
        if (!hasTrainerRole(request)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only trainers can perform this action");
        }
    }

    private void assertAdmin(HttpServletRequest request) {
        if (!hasAdminRole(request)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only admins can perform this action");
        }
    }

    private boolean hasTrainerRole(HttpServletRequest request) {
        return hasRole(request, "TRAINER");
    }

    private boolean hasAdminRole(HttpServletRequest request) {
        return hasRole(request, "ADMIN");
    }

    private boolean hasRole(HttpServletRequest request, String role) {
        String token = extractBearerToken(request);
        if (token == null) {
            return false;
        }
        JsonNode payload = extractPayload(token);
        if (payload == null) {
            return false;
        }
        Set<String> roles = extractRoles(payload);
        return roles.contains(role);
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
        String token = header.substring(7).trim();
        return token.isBlank() ? null : token;
    }

    private JsonNode extractPayload(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) {
                return null;
            }
            byte[] decoded = Base64.getUrlDecoder().decode(parts[1]);
            String payloadJson = new String(decoded, StandardCharsets.UTF_8);
            return objectMapper.readTree(payloadJson);
        } catch (Exception ignored) {
            return null;
        }
    }

    private Set<String> extractRoles(JsonNode payload) {
        Set<String> roles = new HashSet<>();
        addRolesFromArray(payload.path("roles"), roles);
        addRolesFromArray(payload.path("realm_access").path("roles"), roles);
        JsonNode resourceAccess = payload.path("resource_access");
        if (resourceAccess.isObject()) {
            Iterator<JsonNode> clients = resourceAccess.elements();
            while (clients.hasNext()) {
                JsonNode clientNode = clients.next();
                addRolesFromArray(clientNode.path("roles"), roles);
            }
        }
        return roles;
    }

    private void addRolesFromArray(JsonNode roleArray, Set<String> roles) {
        if (!roleArray.isArray()) {
            return;
        }
        for (JsonNode roleNode : roleArray) {
            if (roleNode.isTextual()) {
                String normalized = normalizeRole(roleNode.asText());
                if (!normalized.isBlank()) {
                    roles.add(normalized);
                }
            }
        }
    }

    private String normalizeRole(String role) {
        if (role == null) {
            return "";
        }
        String normalized = role.trim().toUpperCase(Locale.ROOT);
        if (normalized.startsWith("ROLE_")) {
            normalized = normalized.substring("ROLE_".length());
        }
        return normalized;
    }
}
