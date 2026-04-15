package com.example.msforum.services.impl;

import com.example.msforum.dto.PostRequest;
import com.example.msforum.dto.PostResponse;
import com.example.msforum.entities.ContentStatus;
import com.example.msforum.entities.Post;
import com.example.msforum.entities.PostCategory;
import com.example.msforum.exception.ResourceNotFoundException;
import com.example.msforum.repositories.PostRepository;
import com.example.msforum.services.ModerationService;
import com.example.msforum.services.PostService;
import com.example.msforum.services.ReputationService;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;
    private final ReputationService reputationService;
    private final ModerationService moderationService;

    public PostServiceImpl(PostRepository postRepository, ReputationService reputationService, ModerationService moderationService) {
        this.postRepository = postRepository;
        this.reputationService = reputationService;
        this.moderationService = moderationService;
    }

    @Override
    public PostResponse createPost(PostRequest request) {
        Post post = new Post();
        post.setUserId(request.getUserId());
        post.setFormationId(request.getFormationId());
        post.setTitle(request.getTitle());
        post.setContent(request.getContent());
        post.setCategory(request.getCategory() != null ? request.getCategory() : PostCategory.GENERAL);
        if (request.getImageUrl() != null) {
            post.setImageUrl(request.getImageUrl());
        }

        // Moderation
        ContentStatus status = moderationService.moderateContent(post.getTitle() + " " + post.getContent());
        post.setStatus(status);

        Post saved = postRepository.save(post);

        // Only increase reputation if content is APPROVED
        if (status == ContentStatus.APPROVED) {
            reputationService.onPostCreated(saved);
        }

        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PostResponse> getAllPosts(PostCategory category) {
        if (category != null) {
            return postRepository.findByStatusAndCategoryOrderByCreatedAtDesc(ContentStatus.APPROVED, category).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        }
        return postRepository.findByStatusOrderByCreatedAtDesc(ContentStatus.APPROVED).stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PostResponse> getPostsByUserId(String userId) {
        return postRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PostResponse> getPendingPosts() {
        return postRepository.findByStatusInOrderByCreatedAtDesc(java.util.Arrays.asList(ContentStatus.PENDING, ContentStatus.REJECTED)).stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    @Override
    public PostResponse updatePostStatus(Long id, ContentStatus status) {
        Post post = postRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Post not found with id " + id));
        
        ContentStatus oldStatus = post.getStatus();
        post.setStatus(status);
        post.setReviewedByAdmin(true);
        Post saved = postRepository.save(post);

        // If it was not approved before and now it is, trigger reputation
        if (oldStatus != ContentStatus.APPROVED && status == ContentStatus.APPROVED) {
            reputationService.onPostCreated(saved);
        }
        
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PostResponse getPostById(Long id) {
        Post post = postRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Post not found with id " + id));
        return toResponse(post);
    }

    @Override
    public PostResponse updatePost(Long id, PostRequest request) {
        Post post = postRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Post not found with id " + id));
        post.setTitle(request.getTitle());
        post.setContent(request.getContent());
        if (request.getFormationId() != null) {
            post.setFormationId(request.getFormationId());
        }
        if (request.getCategory() != null) {
            post.setCategory(request.getCategory());
        }
        if (request.getImageUrl() != null) {
            post.setImageUrl(request.getImageUrl());
        }
        Post saved = postRepository.save(post);
        return toResponse(saved);
    }

    @Override
    public void deletePost(Long id) {
        Post post = postRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Post not found with id " + id));
        postRepository.delete(post);
    }

    private PostResponse toResponse(Post post) {
        return PostResponse.builder()
            .id(post.getId())
            .userId(post.getUserId())
            .formationId(post.getFormationId())
            .title(post.getTitle())
            .content(post.getContent())
            .bestAnswerCommentId(post.getBestAnswerCommentId())
            .createdAt(post.getCreatedAt())
            .status(post.getStatus())
            .reviewedByAdmin(post.getReviewedByAdmin())
            .category(post.getCategory())
            .imageUrl(post.getImageUrl())
            .build();
    }
}
