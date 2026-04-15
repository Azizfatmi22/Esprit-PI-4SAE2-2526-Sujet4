package com.example.msforum.services.impl;

import com.example.msforum.dto.CommentRequest;
import com.example.msforum.dto.CommentResponse;
import com.example.msforum.entities.Comment;
import com.example.msforum.entities.ContentStatus;
import com.example.msforum.entities.Post;
import com.example.msforum.exception.ResourceNotFoundException;
import com.example.msforum.repositories.CommentRepository;
import com.example.msforum.repositories.PostRepository;
import com.example.msforum.services.CommentService;
import com.example.msforum.services.ModerationService;
import com.example.msforum.services.ReputationService;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final ReputationService reputationService;
    private final ModerationService moderationService;

    public CommentServiceImpl(CommentRepository commentRepository, PostRepository postRepository,
                              ReputationService reputationService, ModerationService moderationService) {
        this.commentRepository = commentRepository;
        this.postRepository = postRepository;
        this.reputationService = reputationService;
        this.moderationService = moderationService;
    }

    @Override
    public CommentResponse addComment(Long postId, CommentRequest request) {
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new ResourceNotFoundException("Post not found with id " + postId));
        Comment comment = new Comment();
        comment.setPost(post);
        comment.setUserId(request.getUserId());
        comment.setContent(request.getContent());

        // Moderation
        ContentStatus status = moderationService.moderateContent(comment.getContent());
        comment.setStatus(status);

        Comment saved = commentRepository.save(comment);

        // Only increase reputation if content is APPROVED
        if (status == ContentStatus.APPROVED) {
            reputationService.onCommentCreated(saved);
        }

        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommentResponse> getCommentsByPost(Long postId, String currentUserId) {
        return commentRepository.findByPostIdOrderByCreatedAtAsc(postId).stream()
            .filter(c -> c.getStatus() == ContentStatus.APPROVED || (currentUserId != null && c.getUserId().equals(currentUserId)))
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommentResponse> getPendingComments() {
        return commentRepository.findByStatusInOrderByCreatedAtDesc(java.util.Arrays.asList(ContentStatus.PENDING, ContentStatus.REJECTED)).stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    @Override
    public CommentResponse updateCommentStatus(Long id, ContentStatus status) {
        Comment comment = commentRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Comment not found with id " + id));
        
        ContentStatus oldStatus = comment.getStatus();
        comment.setStatus(status);
        comment.setReviewedByAdmin(true);
        Comment saved = commentRepository.save(comment);

        // If it was not approved before and now it is, trigger reputation
        if (oldStatus != ContentStatus.APPROVED && status == ContentStatus.APPROVED) {
            reputationService.onCommentCreated(saved);
        }
        
        return toResponse(saved);
    }

    @Override
    public CommentResponse markBestAnswer(Long postId, Long commentId) {
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new ResourceNotFoundException("Post not found with id " + postId));
        Comment comment = commentRepository.findByIdAndPostId(commentId, postId)
            .orElseThrow(() -> new ResourceNotFoundException("Comment not found with id " + commentId));
        Optional<Comment> previousBest = commentRepository.findByPostIdAndIsBestAnswerTrue(postId);
        if (previousBest.isPresent() && previousBest.get().getId().equals(commentId)) {
            return toResponse(comment);
        }
        Comment previous = null;
        if (previousBest.isPresent()) {
            previous = previousBest.get();
            previous.setIsBestAnswer(false);
            commentRepository.save(previous);
        }
        comment.setIsBestAnswer(true);
        Comment saved = commentRepository.save(comment);
        post.setBestAnswerCommentId(saved.getId());
        postRepository.save(post);
        reputationService.onBestAnswerSelected(post, previous, saved);
        return toResponse(saved);
    }

    private CommentResponse toResponse(Comment comment) {
        return CommentResponse.builder()
            .id(comment.getId())
            .postId(comment.getPost().getId())
            .userId(comment.getUserId())
            .content(comment.getContent())
            .isBestAnswer(Boolean.TRUE.equals(comment.getIsBestAnswer()))
            .createdAt(comment.getCreatedAt())
            .status(comment.getStatus())
            .reviewedByAdmin(comment.getReviewedByAdmin())
            .build();
    }
}
