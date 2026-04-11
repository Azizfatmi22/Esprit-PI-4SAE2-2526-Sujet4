package org.example.msreportingcertification.services.impl;

import lombok.RequiredArgsConstructor;
import org.example.msreportingcertification.dto.EvaluationResultDTO;
import org.example.msreportingcertification.entities.EvaluationHistory;
import org.example.msreportingcertification.entities.VigilanceStatus;
import org.example.msreportingcertification.services.interfaces.IBadgeService;
import org.example.msreportingcertification.services.interfaces.IGamificationService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EvaluationResultListener {

    private final EvaluationHistoryImpl evaluationHistoryService;
    private final IGamificationService gamificationService;
    private final IBadgeService badgeService;

    @RabbitListener(
            queues = "evaluation_queue",
            containerFactory = "rabbitListenerContainerFactory"
    )
    public void handleResult(EvaluationResultDTO dto) {
        System.out.println("📩 Réception d'un résultat pour : " + dto.getEvaluationTitle());
        EvaluationHistory savedHistory = evaluationHistoryService.saveResultFromDto(dto);

        if (savedHistory.getVigilanceStatus() == VigilanceStatus.CLEAN) {
            gamificationService.processGamification(savedHistory);
            badgeService.processBadgeAttribution(savedHistory);
            System.out.println("🎮 Gamification appliquée : " + savedHistory.getLearnerName());
        } else {
            System.out.println("🚫 Gamification suspendue : Statut " + savedHistory.getVigilanceStatus());
        }

        if (savedHistory.getVigilanceStatus() == VigilanceStatus.SUSPICIOUS) {
            System.out.println("⚠️ ATTENTION : Tentative marquée comme SUSPICIEUSE (Trop rapide)");
        }

        // Logique de certification simplifiée
        if (Boolean.TRUE.equals(savedHistory.getIsPassed()) &&
                savedHistory.getVigilanceStatus() != VigilanceStatus.BANNED) {
            System.out.println("🎓 Éligible pour certification.");
        }
    }
}
