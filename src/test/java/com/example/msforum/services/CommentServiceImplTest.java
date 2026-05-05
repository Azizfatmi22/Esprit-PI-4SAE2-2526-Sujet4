package com.example.msforum.services;

import com.example.msforum.dto.CommentRequest;
import com.example.msforum.dto.CommentResponse;
import com.example.msforum.entities.Comment;
import com.example.msforum.entities.ContentStatus;
import com.example.msforum.entities.Post;
import com.example.msforum.exception.ResourceNotFoundException;
import com.example.msforum.repositories.CommentRepository;
import com.example.msforum.repositories.PostRepository;
import com.example.msforum.services.impl.CommentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceImplTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private ReputationService reputationService;

    @Mock
    private ModerationService moderationService;

    @InjectMocks
    private CommentServiceImpl commentService;

    private Post post;
    private Comment comment;
    private CommentRequest commentRequest;

    @BeforeEach
    void setUp() {
        post = new Post();
        post.setId(1L);
        post.setUserId("author1");

        comment = new Comment();
        comment.setId(10L);
        comment.setPost(post);
        comment.setUserId("user1");
        comment.setContent("Great post!");
        comment.setStatus(ContentStatus.APPROVED);
        comment.setIsBestAnswer(false);
        comment.setReviewedByAdmin(false);

        commentRequest = new CommentRequest();
        commentRequest.setUserId("user1");
        commentRequest.setContent("Great post!");
    }

    // ── addComment ──────────────────────────────────────────────────────────

    @Test
    void addComment_approvedContent_savesAndGrantsReputation() {
        when(postRepository.findById(1L)).thenReturn(Optional.of(post));
        when(moderationService.moderateContent(anyString())).thenReturn(ContentStatus.APPROVED);
        when(commentRepository.save(any(Comment.class))).thenReturn(comment);

        CommentResponse response = commentService.addComment(1L, commentRequest);

        assertThat(response.getContent()).isEqualTo("Great post!");
        assertThat(response.getStatus()).isEqualTo(ContentStatus.APPROVED);
        verify(reputationService).onCommentCreated(comment);
    }

    @Test
    void addComment_pendingContent_savesWithoutGrantingReputation() {
        Comment pendingComment = new Comment();
        pendingComment.setId(11L);
        pendingComment.setPost(post);
        pendingComment.setUserId("user1");
        pendingComment.setContent("Great post!");
        pendingComment.setStatus(ContentStatus.PENDING);
        pendingComment.setIsBestAnswer(false);
        pendingComment.setReviewedByAdmin(false);

        when(postRepository.findById(1L)).thenReturn(Optional.of(post));
        when(moderationService.moderateContent(anyString())).thenReturn(ContentStatus.PENDING);
        when(commentRepository.save(any(Comment.class))).thenReturn(pendingComment);

        CommentResponse response = commentService.addComment(1L, commentRequest);

        assertThat(response.getStatus()).isEqualTo(ContentStatus.PENDING);
        verify(reputationService, never()).onCommentCreated(any());
    }

    @Test
    void addComment_postNotFound_throwsException() {
        when(postRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.addComment(99L, commentRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    // ── getCommentsByPost ────────────────────────────────────────────────────

    @Test
    void getCommentsByPost_returnsApprovedComments() {
        Comment rejectedComment = new Comment();
        rejectedComment.setId(12L);
        rejectedComment.setPost(post);
        rejectedComment.setUserId("user2");
        rejectedComment.setContent("Spam");
        rejectedComment.setStatus(ContentStatus.REJECTED);
        rejectedComment.setIsBestAnswer(false);
        rejectedComment.setReviewedByAdmin(false);

        when(commentRepository.findByPostIdOrderByCreatedAtAsc(1L))
                .thenReturn(Arrays.asList(comment, rejectedComment));

        List<CommentResponse> result = commentService.getCommentsByPost(1L, null);

        // Only the APPROVED comment should be returned
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getContent()).isEqualTo("Great post!");
    }

    @Test
    void getCommentsByPost_currentUserSeesOwnPendingComments() {
        Comment pendingComment = new Comment();
        pendingComment.setId(13L);
        pendingComment.setPost(post);
        pendingComment.setUserId("user1");
        pendingComment.setContent("My pending comment");
        pendingComment.setStatus(ContentStatus.PENDING);
        pendingComment.setIsBestAnswer(false);
        pendingComment.setReviewedByAdmin(false);

        when(commentRepository.findByPostIdOrderByCreatedAtAsc(1L))
                .thenReturn(Arrays.asList(comment, pendingComment));

        // When currentUserId is the same as the comment author, pending comments are visible
        List<CommentResponse> result = commentService.getCommentsByPost(1L, "user1");

        assertThat(result).hasSize(2);
    }

    // ── updateCommentStatus ──────────────────────────────────────────────────

    @Test
    void updateCommentStatus_pendingToApproved_triggersReputation() {
        comment.setStatus(ContentStatus.PENDING);
        when(commentRepository.findById(10L)).thenReturn(Optional.of(comment));
        when(commentRepository.save(any(Comment.class))).thenAnswer(inv -> inv.getArgument(0));

        CommentResponse response = commentService.updateCommentStatus(10L, ContentStatus.APPROVED);

        assertThat(response.getStatus()).isEqualTo(ContentStatus.APPROVED);
        assertThat(response.getReviewedByAdmin()).isTrue();
        verify(reputationService).onCommentCreated(any(Comment.class));
    }

    @Test
    void updateCommentStatus_approvedToApproved_doesNotDoubleGrantReputation() {
        comment.setStatus(ContentStatus.APPROVED);
        when(commentRepository.findById(10L)).thenReturn(Optional.of(comment));
        when(commentRepository.save(any(Comment.class))).thenAnswer(inv -> inv.getArgument(0));

        commentService.updateCommentStatus(10L, ContentStatus.APPROVED);

        verify(reputationService, never()).onCommentCreated(any());
    }

    @Test
    void updateCommentStatus_nonExistingId_throwsException() {
        when(commentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.updateCommentStatus(99L, ContentStatus.APPROVED))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── markBestAnswer ───────────────────────────────────────────────────────

    @Test
    void markBestAnswer_newBestAnswer_marksItAndUpdatesPost() {
        when(postRepository.findById(1L)).thenReturn(Optional.of(post));
        when(commentRepository.findByIdAndPostId(10L, 1L)).thenReturn(Optional.of(comment));
        when(commentRepository.findByPostIdAndIsBestAnswerTrue(1L)).thenReturn(Optional.empty());
        when(commentRepository.save(any(Comment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(postRepository.save(any(Post.class))).thenReturn(post);

        CommentResponse response = commentService.markBestAnswer(1L, 10L);

        assertThat(response.getIsBestAnswer()).isTrue();
        verify(reputationService).onBestAnswerSelected(eq(post), isNull(), any(Comment.class));
    }

    @Test
    void markBestAnswer_alreadyBestAnswer_returnsUnchanged() {
        comment.setIsBestAnswer(true);
        when(postRepository.findById(1L)).thenReturn(Optional.of(post));
        when(commentRepository.findByIdAndPostId(10L, 1L)).thenReturn(Optional.of(comment));
        when(commentRepository.findByPostIdAndIsBestAnswerTrue(1L)).thenReturn(Optional.of(comment));

        CommentResponse response = commentService.markBestAnswer(1L, 10L);

        assertThat(response.getIsBestAnswer()).isTrue();
        // No reputation update for same comment
        verify(reputationService, never()).onBestAnswerSelected(any(), any(), any());
    }

    @Test
    void markBestAnswer_replacingPreviousBestAnswer_updatesBoth() {
        Comment previousBest = new Comment();
        previousBest.setId(9L);
        previousBest.setPost(post);
        previousBest.setUserId("user2");
        previousBest.setContent("Previous best");
        previousBest.setStatus(ContentStatus.APPROVED);
        previousBest.setIsBestAnswer(true);
        previousBest.setReviewedByAdmin(false);

        when(postRepository.findById(1L)).thenReturn(Optional.of(post));
        when(commentRepository.findByIdAndPostId(10L, 1L)).thenReturn(Optional.of(comment));
        when(commentRepository.findByPostIdAndIsBestAnswerTrue(1L)).thenReturn(Optional.of(previousBest));
        when(commentRepository.save(any(Comment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(postRepository.save(any(Post.class))).thenReturn(post);

        commentService.markBestAnswer(1L, 10L);

        verify(reputationService).onBestAnswerSelected(eq(post), eq(previousBest), eq(comment));
    }

    @Test
    void markBestAnswer_commentNotFound_throwsException() {
        when(postRepository.findById(1L)).thenReturn(Optional.of(post));
        when(commentRepository.findByIdAndPostId(99L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.markBestAnswer(1L, 99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── getPendingComments ───────────────────────────────────────────────────

    @Test
    void getPendingComments_returnsPendingAndRejectedComments() {
        comment.setStatus(ContentStatus.PENDING);
        when(commentRepository.findByStatusInOrderByCreatedAtDesc(
                Arrays.asList(ContentStatus.PENDING, ContentStatus.REJECTED)))
                .thenReturn(List.of(comment));

        List<CommentResponse> result = commentService.getPendingComments();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(ContentStatus.PENDING);
    }
}
