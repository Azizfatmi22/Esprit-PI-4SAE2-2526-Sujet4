package org.example.msreportingcertification.services.impl;

import lombok.RequiredArgsConstructor;
import org.example.msreportingcertification.dto.EvaluationResultDTO;
import org.example.msreportingcertification.entities.EvaluationHistory;
import org.example.msreportingcertification.entities.VigilanceStatus;
import org.example.msreportingcertification.repositories.EvaluationHistoryRepository;
import org.example.msreportingcertification.services.interfaces.IBadgeService;
import org.example.msreportingcertification.services.interfaces.IEvaluationHistoryService;
import org.example.msreportingcertification.services.interfaces.IGamificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EvaluationHistoryImpl implements IEvaluationHistoryService {
    private final EvaluationHistoryRepository evaluationHistoryRepository;
    private final IGamificationService gamificationService;
    private final IBadgeService badgeService;


    @Override
    public List<EvaluationHistory> getResultsByEvaluation(Long evaluationId) {
        return evaluationHistoryRepository.findByEvaluationId(evaluationId);
    }

    public VigilanceStatus detectVigilanceStatus(int timeSpentSeconds, int durationInMinutes) {
        long totalAllowedSeconds = durationInMinutes * 60L;
        long minimumThreshold = totalAllowedSeconds / 10; // 10% du temps

        if (timeSpentSeconds < minimumThreshold) {
            return VigilanceStatus.SUSPICIOUS;
        }
        return VigilanceStatus.CLEAN;
    }

    @Override
    @Transactional
    public void updateStatus(Long historyId, VigilanceStatus newStatus) {
        evaluationHistoryRepository.findById(historyId).ifPresent(history -> {
            VigilanceStatus oldStatus = history.getVigilanceStatus();
            history.setVigilanceStatus(newStatus);
            evaluationHistoryRepository.save(history);


            if (oldStatus != VigilanceStatus.CLEAN && newStatus == VigilanceStatus.CLEAN) {
                gamificationService.processGamification(history);
                badgeService.processBadgeAttribution(history);
            }

            // Cas 2 : On découvre une fraude après coup -> On retire les points (Optionnel/Avancé)
            if (oldStatus == VigilanceStatus.CLEAN && (newStatus == VigilanceStatus.BANNED || newStatus == VigilanceStatus.SUSPICIOUS)) {
                gamificationService.rollbackGamification(history); // Méthode à créer pour soustraire
                badgeService.revokeBadges(historyId);
            }
        });
    }

    @Override
    public EvaluationHistory saveResultFromDto(EvaluationResultDTO dto) {
        VigilanceStatus autoStatus = detectVigilanceStatus(dto.getTimeSpentSeconds(), dto.getDuration());
        EvaluationHistory history = new EvaluationHistory();
        history.setEvaluationId(dto.getEvaluationId());
        history.setLearnerId(dto.getLearnerId());
        history.setLearnerName(dto.getLearnerName());
        history.setEvaluationTitle(dto.getEvaluationTitle());
        history.setType(dto.getType());
        history.setScoreObtained(dto.getScoreObtained());
        history.setTotalPossiblePoints(dto.getTotalPossiblePoints());
        history.setDuration(dto.getDuration());
        history.setTimeSpentSeconds(dto.getTimeSpentSeconds());
        history.setPercentage(dto.getPercentage());
        history.setIsPassed(dto.getIsPassed());
        history.setVigilanceStatus(autoStatus);

        return evaluationHistoryRepository.save(history);
    }





}