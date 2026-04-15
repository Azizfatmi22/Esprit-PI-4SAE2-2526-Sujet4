package com.example.msforum.repositories;

import com.example.msforum.entities.ReputationEvent;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReputationEventRepository extends JpaRepository<ReputationEvent, Long> {
    List<ReputationEvent> findTop20ByUserIdOrderByCreatedAtDesc(String userId);
}
