package com.example.msforum.repositories;

import com.example.msforum.entities.Comment;
import com.example.msforum.entities.ContentStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByPostIdOrderByCreatedAtAsc(Long postId);

    Optional<Comment> findByIdAndPostId(Long id, Long postId);

    Optional<Comment> findByPostIdAndIsBestAnswerTrue(Long postId);

    List<Comment> findByStatusOrderByCreatedAtDesc(ContentStatus status);

    List<Comment> findByStatusInOrderByCreatedAtDesc(List<ContentStatus> statuses);
}
