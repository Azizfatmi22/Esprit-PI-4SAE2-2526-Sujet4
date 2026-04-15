package org.example.msreportingcertification.Services;


import org.example.msreportingcertification.dto.EvaluationResultDTO;
import org.example.msreportingcertification.entities.EvaluationHistory;
import org.example.msreportingcertification.entities.VigilanceStatus;
import org.example.msreportingcertification.services.impl.EvaluationHistoryImpl;
import org.example.msreportingcertification.services.impl.EvaluationResultListener;
import org.example.msreportingcertification.services.interfaces.IBadgeService;
import org.example.msreportingcertification.services.interfaces.IGamificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EvaluationResultListenerTest {

    @Mock
    private EvaluationHistoryImpl evaluationHistoryService;
    @Mock
    private IGamificationService gamificationService;
    @Mock
    private IBadgeService badgeService;

    @InjectMocks
    private EvaluationResultListener evaluationResultListener;

    private EvaluationResultDTO dto;
    private EvaluationHistory history;

    @BeforeEach
    void setUp() {
        dto = EvaluationResultDTO.builder()
                .evaluationTitle("Java Mastery")
                .learnerName("Omar Aguil")
                .isPassed(true)
                .build();

        history = new EvaluationHistory();
        history.setLearnerName("Omar Aguil");
    }

    @Test
    void handleResult_ShouldApplyGamification_WhenStatusIsClean() {
        // Arrange
        history.setVigilanceStatus(VigilanceStatus.CLEAN);
        when(evaluationHistoryService.saveResultFromDto(any(EvaluationResultDTO.class))).thenReturn(history);

        // Act
        evaluationResultListener.handleResult(dto);

        // Assert
        verify(evaluationHistoryService, times(1)).saveResultFromDto(dto);
        verify(gamificationService, times(1)).processGamification(history);
        verify(badgeService, times(1)).processBadgeAttribution(history);
    }

    @Test
    void handleResult_ShouldSuspendGamification_WhenStatusIsSuspicious() {
        // Arrange
        history.setVigilanceStatus(VigilanceStatus.SUSPICIOUS);
        when(evaluationHistoryService.saveResultFromDto(any(EvaluationResultDTO.class))).thenReturn(history);

        // Act
        evaluationResultListener.handleResult(dto);

        // Assert
        verify(evaluationHistoryService, times(1)).saveResultFromDto(dto);
        // Gamification et Badges ne doivent JAMAIS être appelés si c'est suspect
        verify(gamificationService, never()).processGamification(any());
        verify(badgeService, never()).processBadgeAttribution(any());
    }

    @Test
    void handleResult_ShouldHandleBannedStatus() {
        // Arrange
        history.setVigilanceStatus(VigilanceStatus.BANNED);
        when(evaluationHistoryService.saveResultFromDto(any(EvaluationResultDTO.class))).thenReturn(history);

        // Act
        evaluationResultListener.handleResult(dto);

        // Assert
        verify(gamificationService, never()).processGamification(any());
        // On vérifie que le code ne plante pas avec un statut extrême
        verify(evaluationHistoryService).saveResultFromDto(dto);
    }
}