package com.example.msforum.services;

import com.example.msforum.dto.ReactionCountResponse;
import com.example.msforum.dto.ReactionRequest;

public interface ReactionService {
    void reactToPost(Long postId, ReactionRequest request);

    ReactionCountResponse getReactionCounts(Long postId);
}
