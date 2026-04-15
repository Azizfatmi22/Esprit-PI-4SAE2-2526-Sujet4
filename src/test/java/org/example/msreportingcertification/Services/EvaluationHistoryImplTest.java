package org.example.msreportingcertification.Services;


import org.example.msreportingcertification.dto.EvaluationResultDTO;
import org.example.msreportingcertification.entities.EvaluationHistory;
import org.example.msreportingcertification.entities.VigilanceStatus;
import org.example.msreportingcertification.repositories.EvaluationHistoryRepository;
import org.example.msreportingcertification.services.impl.EvaluationHistoryImpl;
import org.example.msreportingcertification.services.interfaces.IBadgeService;
import org.example.msreportingcertification.services.interfaces.IGamificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EvaluationHistoryImplTest {

    @Mock
    private EvaluationHistoryRepository evaluationHistoryRepository;
    @Mock
    private IGamificationService gamificationService;
    @Mock
    private IBadgeService badgeService;

    @InjectMocks
    private EvaluationHistoryImpl evaluationHistoryService;

    private EvaluationHistory history;

    @BeforeEach
    void setUp() {
        history = new EvaluationHistory();
        history.setId(1L);
        history.setLearnerId("OMAR-2026");
        history.setVigilanceStatus(VigilanceStatus.SUSPICIOUS);
    }

    @Test
    void detectVigilanceStatus_ShouldReturnSuspicious_WhenTimeIsTooShort() {
        // 60 min = 3600s. Seuil 10% = 360s. 100s est < 360s.
        VigilanceStatus status = evaluationHistoryService.detectVigilanceStatus(100, 60);
        assertThat(status).isEqualTo(VigilanceStatus.SUSPICIOUS);
    }

    @Test
    void detectVigilanceStatus_ShouldReturnClean_WhenTimeIsCorrect() {
        // 500s est > 360s
        VigilanceStatus status = evaluationHistoryService.detectVigilanceStatus(500, 60);
        assertThat(status).isEqualTo(VigilanceStatus.CLEAN);
    }

    @Test
    void updateStatus_ShouldTriggerRewards_WhenStatusChangesToClean() {
        // Arrange
        when(evaluationHistoryRepository.findById(1L)).thenReturn(Optional.of(history));

        // Act
        evaluationHistoryService.updateStatus(1L, VigilanceStatus.CLEAN);

        // Assert
        assertThat(history.getVigilanceStatus()).isEqualTo(VigilanceStatus.CLEAN);
        verify(gamificationService, times(1)).processGamification(history);
        verify(badgeService, times(1)).processBadgeAttribution(history);
    }

    @Test
    void updateStatus_ShouldRevokeRewards_WhenFraudDetectedLater() {
        // Arrange
        history.setVigilanceStatus(VigilanceStatus.CLEAN);
        when(evaluationHistoryRepository.findById(1L)).thenReturn(Optional.of(history));

        // Act
        evaluationHistoryService.updateStatus(1L, VigilanceStatus.BANNED);

        // Assert
        verify(gamificationService, times(1)).rollbackGamification(history);
        verify(badgeService, times(1)).revokeBadges(1L);
    }

    @Test
    void saveResultFromDto_ShouldAutoDetectStatus() {
        // Arrange
        EvaluationResultDTO dto = EvaluationResultDTO.builder()
                .evaluationId(10L)
                .timeSpentSeconds(10) // Très rapide
                .duration(60)
                .learnerName("Omar")
                .build();

        when(evaluationHistoryRepository.save(any(EvaluationHistory.class))).thenAnswer(i -> i.getArguments()[0]);

        // Act
        EvaluationHistory result = evaluationHistoryService.saveResultFromDto(dto);

        // Assert
        assertThat(result.getVigilanceStatus()).isEqualTo(VigilanceStatus.SUSPICIOUS);
        verify(evaluationHistoryRepository).save(any(EvaluationHistory.class));
    }
}
