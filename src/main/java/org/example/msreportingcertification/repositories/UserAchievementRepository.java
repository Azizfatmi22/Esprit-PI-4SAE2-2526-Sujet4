package org.example.msreportingcertification.repositories;

import org.example.msreportingcertification.entities.UserAchievement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserAchievementRepository extends JpaRepository<UserAchievement, Long> {
    boolean existsByLearnerIdAndBadgeId(String learnerId, Long badgeId);
    void deleteByEvaluationHistoryId(Long evaluationHistoryId);
    List<UserAchievement> findByLearnerId(String learnerId);
}
