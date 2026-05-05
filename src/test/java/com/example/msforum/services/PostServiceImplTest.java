package com.example.msforum.services;

import com.example.msforum.dto.PostRequest;
import com.example.msforum.dto.PostResponse;
import com.example.msforum.entities.ContentStatus;
import com.example.msforum.entities.Post;
import com.example.msforum.entities.PostCategory;
import com.example.msforum.exception.ResourceNotFoundException;
import com.example.msforum.repositories.PostRepository;
import com.example.msforum.services.impl.PostServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostServiceImplTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private ReputationService reputationService;

    @Mock
    private ModerationService moderationService;

    @InjectMocks
    private PostServiceImpl postService;

    private Post post;
    private PostRequest postRequest;

    @BeforeEach
    void setUp() {
        post = new Post();
        post.setId(1L);
        post.setUserId("user1");
        post.setTitle("Test Title");
        post.setContent("Test Content");
        post.setCategory(PostCategory.GENERAL);
        post.setStatus(ContentStatus.APPROVED);
        post.setReviewedByAdmin(false);

        postRequest = new PostRequest();
        postRequest.setUserId("user1");
        postRequest.setTitle("Test Title");
        postRequest.setContent("Test Content");
        postRequest.setCategory(PostCategory.GENERAL);
    }

    // ── createPost ──────────────────────────────────────────────────────────

    @Test
    void createPost_approvedContent_savesAndGrantsReputation() {
        when(moderationService.moderateContent(anyString())).thenReturn(ContentStatus.APPROVED);
        when(postRepository.save(any(Post.class))).thenReturn(post);

        PostResponse response = postService.createPost(postRequest);

        assertThat(response.getTitle()).isEqualTo("Test Title");
        assertThat(response.getStatus()).isEqualTo(ContentStatus.APPROVED);
        verify(reputationService).onPostCreated(post);
        verify(postRepository).save(any(Post.class));
    }

    @Test
    void createPost_pendingContent_savesWithoutGrantingReputation() {
        when(moderationService.moderateContent(anyString())).thenReturn(ContentStatus.PENDING);
        Post pendingPost = new Post();
        pendingPost.setId(2L);
        pendingPost.setUserId("user1");
        pendingPost.setTitle("Test Title");
        pendingPost.setContent("Test Content");
        pendingPost.setStatus(ContentStatus.PENDING);
        when(postRepository.save(any(Post.class))).thenReturn(pendingPost);

        PostResponse response = postService.createPost(postRequest);

        assertThat(response.getStatus()).isEqualTo(ContentStatus.PENDING);
        verify(reputationService, never()).onPostCreated(any());
    }

    @Test
    void createPost_nullCategory_defaultsToGeneral() {
        postRequest.setCategory(null);
        when(moderationService.moderateContent(anyString())).thenReturn(ContentStatus.APPROVED);
        when(postRepository.save(any(Post.class))).thenAnswer(inv -> {
            Post p = inv.getArgument(0);
            p.setId(1L);
            return p;
        });

        PostResponse response = postService.createPost(postRequest);

        assertThat(response.getCategory()).isEqualTo(PostCategory.GENERAL);
    }

    // ── getAllPosts ──────────────────────────────────────────────────────────

    @Test
    void getAllPosts_noCategory_returnsAllApprovedPosts() {
        when(postRepository.findByStatusOrderByCreatedAtDesc(ContentStatus.APPROVED))
                .thenReturn(List.of(post));

        List<PostResponse> result = postService.getAllPosts(null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUserId()).isEqualTo("user1");
    }

    @Test
    void getAllPosts_withCategory_returnsFilteredPosts() {
        when(postRepository.findByStatusAndCategoryOrderByCreatedAtDesc(ContentStatus.APPROVED, PostCategory.GENERAL))
                .thenReturn(List.of(post));

        List<PostResponse> result = postService.getAllPosts(PostCategory.GENERAL);

        assertThat(result).hasSize(1);
        verify(postRepository).findByStatusAndCategoryOrderByCreatedAtDesc(ContentStatus.APPROVED, PostCategory.GENERAL);
    }

    // ── getPostById ─────────────────────────────────────────────────────────

    @Test
    void getPostById_existingId_returnsPost() {
        when(postRepository.findById(1L)).thenReturn(Optional.of(post));

        PostResponse response = postService.getPostById(1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getTitle()).isEqualTo("Test Title");
    }

    @Test
    void getPostById_nonExistingId_throwsException() {
        when(postRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postService.getPostById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    // ── updatePost ──────────────────────────────────────────────────────────

    @Test
    void updatePost_existingId_updatesFields() {
        PostRequest updateRequest = new PostRequest();
        updateRequest.setTitle("Updated Title");
        updateRequest.setContent("Updated Content");
        updateRequest.setCategory(PostCategory.TECHNOLOGY);

        when(postRepository.findById(1L)).thenReturn(Optional.of(post));
        when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));

        PostResponse response = postService.updatePost(1L, updateRequest);

        assertThat(response.getTitle()).isEqualTo("Updated Title");
        assertThat(response.getContent()).isEqualTo("Updated Content");
        assertThat(response.getCategory()).isEqualTo(PostCategory.TECHNOLOGY);
    }

    @Test
    void updatePost_nonExistingId_throwsException() {
        when(postRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postService.updatePost(99L, postRequest))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── updatePostStatus ────────────────────────────────────────────────────

    @Test
    void updatePostStatus_pendingToApproved_triggersReputation() {
        post.setStatus(ContentStatus.PENDING);
        when(postRepository.findById(1L)).thenReturn(Optional.of(post));
        when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));

        PostResponse response = postService.updatePostStatus(1L, ContentStatus.APPROVED);

        assertThat(response.getStatus()).isEqualTo(ContentStatus.APPROVED);
        assertThat(response.getReviewedByAdmin()).isTrue();
        verify(reputationService).onPostCreated(any(Post.class));
    }

    @Test
    void updatePostStatus_approvedToApproved_doesNotDoubleGrantReputation() {
        post.setStatus(ContentStatus.APPROVED);
        when(postRepository.findById(1L)).thenReturn(Optional.of(post));
        when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));

        postService.updatePostStatus(1L, ContentStatus.APPROVED);

        verify(reputationService, never()).onPostCreated(any());
    }

    @Test
    void updatePostStatus_nonExistingId_throwsException() {
        when(postRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postService.updatePostStatus(99L, ContentStatus.APPROVED))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── deletePost ──────────────────────────────────────────────────────────

    @Test
    void deletePost_existingId_deletesPost() {
        when(postRepository.findById(1L)).thenReturn(Optional.of(post));

        postService.deletePost(1L);

        verify(postRepository).delete(post);
    }

    @Test
    void deletePost_nonExistingId_throwsException() {
        when(postRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postService.deletePost(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── getPostsByUserId ─────────────────────────────────────────────────────

    @Test
    void getPostsByUserId_returnsUserPosts() {
        when(postRepository.findByUserIdOrderByCreatedAtDesc("user1")).thenReturn(List.of(post));

        List<PostResponse> result = postService.getPostsByUserId("user1");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUserId()).isEqualTo("user1");
    }

    // ── getPendingPosts ──────────────────────────────────────────────────────

    @Test
    void getPendingPosts_returnsPendingAndRejectedPosts() {
        Post pendingPost = new Post();
        pendingPost.setId(2L);
        pendingPost.setStatus(ContentStatus.PENDING);
        pendingPost.setUserId("user2");

        when(postRepository.findByStatusInOrderByCreatedAtDesc(
                Arrays.asList(ContentStatus.PENDING, ContentStatus.REJECTED)))
                .thenReturn(List.of(pendingPost));

        List<PostResponse> result = postService.getPendingPosts();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(ContentStatus.PENDING);
    }
}
