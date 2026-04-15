package com.example.msforum.services.impl;

import com.example.msforum.dto.ReactionCountResponse;
import com.example.msforum.dto.ReactionRequest;
import com.example.msforum.entities.Post;
import com.example.msforum.entities.Reaction;
import com.example.msforum.entities.ReactionType;
import com.example.msforum.exception.ResourceNotFoundException;
import com.example.msforum.repositories.PostRepository;
import com.example.msforum.repositories.ReactionRepository;
import com.example.msforum.services.ReactionService;
import com.example.msforum.services.ReputationService;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ReactionServiceImpl implements ReactionService {

    private final ReactionRepository reactionRepository;
    private final PostRepository postRepository;
    private final ReputationService reputationService;

    public ReactionServiceImpl(ReactionRepository reactionRepository, PostRepository postRepository,
                               ReputationService reputationService) {
        this.reactionRepository = reactionRepository;
        this.postRepository = postRepository;
        this.reputationService = reputationService;
    }

    @Override
    public void reactToPost(Long postId, ReactionRequest request) {
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new ResourceNotFoundException("Post not found with id " + postId));
        Optional<Reaction> previousReaction = reactionRepository.findByPostIdAndUserId(postId, request.getUserId());
        ReactionType previousType = previousReaction.map(Reaction::getType).orElse(null);
        Reaction reaction = previousReaction
            .orElseGet(Reaction::new);
        reaction.setPost(post);
        reaction.setUserId(request.getUserId());
        reaction.setType(request.getType());
        reactionRepository.save(reaction);
        reputationService.onPostReactionChanged(post, request.getUserId(), previousType, request.getType());
    }

    @Override
    @Transactional(readOnly = true)
    public ReactionCountResponse getReactionCounts(Long postId) {
        postRepository.findById(postId)
            .orElseThrow(() -> new ResourceNotFoundException("Post not found with id " + postId));
        long likes = reactionRepository.countByPostIdAndType(postId, ReactionType.LIKE);
        long dislikes = reactionRepository.countByPostIdAndType(postId, ReactionType.DISLIKE);
        return ReactionCountResponse.builder()
            .postId(postId)
            .likeCount(likes)
            .dislikeCount(dislikes)
            .build();
    }
}
