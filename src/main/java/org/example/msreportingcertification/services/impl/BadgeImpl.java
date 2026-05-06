package org.example.msreportingcertification.services.impl;

import lombok.RequiredArgsConstructor;
import org.example.msreportingcertification.entities.Badge;
import org.example.msreportingcertification.entities.EvaluationHistory;
import org.example.msreportingcertification.entities.UserAchievement;
import org.example.msreportingcertification.entities.VigilanceStatus;
import org.example.msreportingcertification.repositories.BadgeRepository;
import org.example.msreportingcertification.repositories.EvaluationHistoryRepository;
import org.example.msreportingcertification.repositories.UserAchievementRepository;
import org.example.msreportingcertification.services.interfaces.IBadgeService;
import org.example.msreportingcertification.services.interfaces.IGamificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BadgeImpl implements IBadgeService {
    private final BadgeRepository badgeRepo;
    private final UserAchievementRepository achievementRepo;
    private final EvaluationHistoryRepository historyRepo;
    private final IGamificationService gamificationService;


    @Transactional
    @Override
    public void processBadgeAttribution(EvaluationHistory history) {
        if (history.getVigilanceStatus() != VigilanceStatus.CLEAN) return;
        Integer currentLevel = gamificationService.getLearnerLevel(history.getLearnerId());
        
        List<Badge> allBadges = badgeRepo.findAll();

        for (Badge badge : allBadges) {
            if (achievementRepo.existsByLearnerIdAndBadgeId(history.getLearnerId(), badge.getId())) {
                continue;
            }

            boolean conditionMet = false;

            switch (badge.getBadgeType()) {
                case SCORE_EXCELLENCE:
                    if (history.getPercentage() != null && history.getPercentage() >= badge.getThreshold())
                        conditionMet = true;
                    break;
                case CERTIFICATE_COUNT:
                    long certCount = historyRepo.countByLearnerIdAndIsPassedAndVigilanceStatus(
                            history.getLearnerId(), true, VigilanceStatus.CLEAN);
                    if (certCount >= badge.getThreshold()) conditionMet = true;
                    break;
                case LEVEL_REACHED:
                    if (currentLevel >= badge.getThreshold()) conditionMet = true;
                    break;
            }

            if (conditionMet) {
              
                UserAchievement achievement = UserAchievement.builder()
                        .learnerId(history.getLearnerId())
                        .badge(badge)
                        .evaluationHistoryId(history.getId())
                        .unlockedAt(LocalDateTime.now())
                        .build();
                achievementRepo.save(achievement);
                
            }
        }
    }

    @Transactional
    @Override
    public void revokeBadges(Long historyId) {
        achievementRepo.deleteByEvaluationHistoryId(historyId);
    }





@Override
public Badge createBadge(Badge badge) {
    return badgeRepo.save(badge);
}

@Override
public List<Badge> getAllBadges() {
    return badgeRepo.findAll();
}

@Override
public Badge getBadgeById(Long id) {
    return badgeRepo.findById(id)
            .orElseThrow(() -> new RuntimeException("Badge non trouvé avec l'id : " + id));
}

@Override
public Badge updateBadge(Long id, Badge badgeDetails) {
    Badge badge = getBadgeById(id);
    badge.setName(badgeDetails.getName());
    badge.setDescription(badgeDetails.getDescription());
    badge.setIconBase64(badgeDetails.getIconBase64());
    badge.setMinScoreRequired(badgeDetails.getMinScoreRequired());
    badge.setMaxTimeAllowed(badgeDetails.getMaxTimeAllowed());
    badge.setCategory(badgeDetails.getCategory());
    badge.setBadgeType(badgeDetails.getBadgeType());
    badge.setThreshold(badgeDetails.getThreshold());
    return badgeRepo.save(badge);
}

@Override
public void deleteBadge(Long id) {
    badgeRepo.deleteById(id);
}

    @Override
    public List<UserAchievement> getLearnerAchievements(String learnerId) {
        return achievementRepo.findByLearnerId(learnerId);
    }

}
