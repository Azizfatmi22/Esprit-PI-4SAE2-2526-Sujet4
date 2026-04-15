package com.example.msforum.repositories;

import com.example.msforum.entities.ContentStatus;
import com.example.msforum.entities.Post;
import com.example.msforum.entities.PostCategory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<Post, Long> {
    List<Post> findAllByOrderByCreatedAtDesc();

    List<Post> findByUserIdOrderByCreatedAtDesc(String userId);

    List<Post> findByStatusOrderByCreatedAtDesc(ContentStatus status);

    List<Post> findByStatusInOrderByCreatedAtDesc(List<ContentStatus> statuses);

    List<Post> findByStatusAndCategoryOrderByCreatedAtDesc(ContentStatus status, PostCategory category);
}
