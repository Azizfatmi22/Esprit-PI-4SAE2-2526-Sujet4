package com.example.msforum.services;

import com.example.msforum.dto.CommentRequest;
import com.example.msforum.dto.CommentResponse;
import com.example.msforum.entities.ContentStatus;
import java.util.List;

public interface CommentService {
    CommentResponse addComment(Long postId, CommentRequest request);

    List<CommentResponse> getCommentsByPost(Long postId, String currentUserId);

    CommentResponse markBestAnswer(Long postId, Long commentId);

    List<CommentResponse> getPendingComments();

    CommentResponse updateCommentStatus(Long id, ContentStatus status);
}
