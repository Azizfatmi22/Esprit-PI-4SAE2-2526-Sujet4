package com.example.msforum.repositories;

import com.example.msforum.entities.UserReputation;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserReputationRepository extends JpaRepository<UserReputation, Long> {
    Optional<UserReputation> findByUserId(String userId);

    List<UserReputation> findAllByOrderByPointsDescUpdatedAtAsc(Pageable pageable);

    long countByPointsGreaterThan(Integer points);
}
