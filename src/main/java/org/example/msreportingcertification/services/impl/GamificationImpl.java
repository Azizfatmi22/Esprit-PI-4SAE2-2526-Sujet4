package org.example.msreportingcertification.services.impl;

import lombok.RequiredArgsConstructor;
import org.example.msreportingcertification.entities.EvaluationHistory;
import org.example.msreportingcertification.entities.LearnerProgression;
import org.example.msreportingcertification.entities.VigilanceStatus;
import org.example.msreportingcertification.repositories.EvaluationHistoryRepository;
import org.example.msreportingcertification.repositories.LearnerProgressionRepository;
import org.example.msreportingcertification.services.interfaces.IGamificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class GamificationImpl implements IGamificationService {

    private final LearnerProgressionRepository progressionRepo;
    private final EvaluationHistoryRepository historyRepo;

    @Transactional
    @Override
    public void processGamification(EvaluationHistory history) {
        long points = calculateXP(history);
        history.setPointsGained((int) points);
        historyRepo.save(history);
        updateProgression(history.getLearnerId(), history.getLearnerName(), points, 1);
    }

    @Transactional
    @Override
    public void rollbackGamification(EvaluationHistory history) {
        long pointsARetirer = -calculateXP(history);
        if (pointsARetirer >0){
            updateProgression(history.getLearnerId(), history.getLearnerName(), pointsARetirer, -1);
            history.setPointsGained(0);
            historyRepo.save(history);
        }

    }

    @Override
    public Integer getLearnerLevel(String learnerId) {
        Long totalXp = progressionRepo.findTotalXpByLearnerId(learnerId);
        System.out.println("________________>Total XP: " + totalXp);

        if (totalXp == null || totalXp <= 0) {
            return 1;
        }
        return (int) (totalXp / 500) + 1;
    }
    private void updateProgression(String learnerId, String learnerName, long points, int certCountChange) {
        LearnerProgression p = progressionRepo.findById(learnerId)
                .orElse(LearnerProgression.builder()
                        .learnerId(learnerId)
                        .learnerName(learnerName)
                        .totalXp(0L)
                        .currentLevel(1)
                        .totalCertificates(0)
                        .lastActivity(LocalDateTime.now())
                        .build());

        p.setTotalXp(Math.max(0, p.getTotalXp() + points));
        p.setCurrentLevel((int) (p.getTotalXp() / 500) + 1);
        int newCertCount = p.getTotalCertificates() + certCountChange;
        p.setTotalCertificates(Math.max(0, newCertCount));
        p.setLastActivity(LocalDateTime.now());

        progressionRepo.save(p);
    }

    private long calculateXP(EvaluationHistory h) {
        if (h.getPercentage() == null) return 0;
        return Math.round(h.getPercentage()) + 50;
    }
}