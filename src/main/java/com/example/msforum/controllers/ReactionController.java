package com.example.msforum.controllers;

import com.example.msforum.dto.ReactionCountResponse;
import com.example.msforum.dto.ReactionRequest;
import com.example.msforum.services.ReactionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/posts/{postId}/reactions")
public class ReactionController {

    private final ReactionService reactionService;

    public ReactionController(ReactionService reactionService) {
        this.reactionService = reactionService;
    }

    /**
     * Sample JSON:
     * {
     *   "userId": 10,
     *   "type": "LIKE"
     * }
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void react(@PathVariable Long postId, @Valid @RequestBody ReactionRequest request) {
        reactionService.reactToPost(postId, request);
    }

    @GetMapping("/count")
    public ReactionCountResponse getCounts(@PathVariable Long postId) {
        return reactionService.getReactionCounts(postId);
    }
}
