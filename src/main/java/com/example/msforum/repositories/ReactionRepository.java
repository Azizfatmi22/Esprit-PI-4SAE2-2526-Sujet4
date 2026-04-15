package com.example.msforum.repositories;

import com.example.msforum.entities.Reaction;
import com.example.msforum.entities.ReactionType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReactionRepository extends JpaRepository<Reaction, Long> {
    Optional<Reaction> findByPostIdAndUserId(Long postId, String userId);

    long countByPostIdAndType(Long postId, ReactionType type);
}
