package com.example.msforum.services;

import com.example.msforum.dto.PostRequest;
import com.example.msforum.dto.PostResponse;
import com.example.msforum.entities.ContentStatus;
import com.example.msforum.entities.PostCategory;
import java.util.List;

public interface PostService {
    PostResponse createPost(PostRequest request);

    List<PostResponse> getAllPosts(PostCategory category);

    List<PostResponse> getPostsByUserId(String userId);

    PostResponse getPostById(Long id);

    PostResponse updatePost(Long id, PostRequest request);

    void deletePost(Long id);

    List<PostResponse> getPendingPosts();

    PostResponse updatePostStatus(Long id, ContentStatus status);
}
