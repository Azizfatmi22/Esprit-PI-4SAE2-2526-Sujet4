package org.example.msreportingcertification.services.interfaces;

import org.example.msreportingcertification.entities.Badge;
import org.example.msreportingcertification.entities.EvaluationHistory;
import org.example.msreportingcertification.entities.UserAchievement;
import org.springframework.transaction.annotation.Transactional;


import java.util.List;

public interface IBadgeService {


    Badge createBadge(Badge badge);
    List<Badge> getAllBadges();
    Badge updateBadge(Long id, Badge badgeDetails);
    void deleteBadge(Long id);
    Badge getBadgeById(Long id);

    @Transactional
    void processBadgeAttribution(EvaluationHistory history);

    @Transactional
    void revokeBadges(Long historyId);

    List<UserAchievement> getLearnerAchievements(String learnerId);
}
