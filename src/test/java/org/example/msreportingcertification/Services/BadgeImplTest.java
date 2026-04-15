package org.example.msreportingcertification.Services;


import org.example.msreportingcertification.entities.*;
import org.example.msreportingcertification.repositories.BadgeRepository;
import org.example.msreportingcertification.repositories.EvaluationHistoryRepository;
import org.example.msreportingcertification.repositories.UserAchievementRepository;
import org.example.msreportingcertification.services.impl.BadgeImpl;
import org.example.msreportingcertification.services.interfaces.IGamificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BadgeImplTest {

    @Mock
    private BadgeRepository badgeRepo;
    @Mock
    private UserAchievementRepository achievementRepo;
    @Mock
    private EvaluationHistoryRepository historyRepo;
    @Mock
    private IGamificationService gamificationService;

    @InjectMocks
    private BadgeImpl badgeService;

    private EvaluationHistory history;
    private Badge excellenceBadge;

    @BeforeEach
    void setUp() {
        history = new EvaluationHistory();
        history.setId(100L);
        history.setLearnerId("OMAR-123");
        history.setVigilanceStatus(VigilanceStatus.CLEAN);
        history.setPercentage(95.0);

        excellenceBadge = Badge.builder()
                .id(1L)
                .name("Excellence Java")
                .badgeType(BadgeType.SCORE_EXCELLENCE)
                .threshold(90) // On utilise double ici si threshold est double
                .build();
    }

    @Test
    void processBadgeAttribution_ShouldUnlockBadge_WhenScoreIsExcellent() {
        // Arrange
        when(badgeRepo.findAll()).thenReturn(List.of(excellenceBadge));
        when(achievementRepo.existsByLearnerIdAndBadgeId("OMAR-123", 1L)).thenReturn(false);
        // On mock le niveau même si ce n'est pas le type SCORE_EXCELLENCE pour éviter les NullPointerException
        when(gamificationService.getLearnerLevel("OMAR-123")).thenReturn(1);

        // Act
        badgeService.processBadgeAttribution(history);

        // Assert
        verify(achievementRepo, times(1)).save(any(UserAchievement.class));
    }

    @Test
    void processBadgeAttribution_ShouldNotUnlock_WhenVigilanceIsNotClean() {
        // Arrange
        history.setVigilanceStatus(VigilanceStatus.SUSPICIOUS);

        // Act
        badgeService.processBadgeAttribution(history);

        // Assert
        verifyNoInteractions(badgeRepo); // Le code doit s'arrêter au premier IF
        verify(achievementRepo, never()).save(any());
    }

    @Test
    void processBadgeAttribution_ShouldUnlock_WhenLevelReached() {
        // Arrange
        Badge levelBadge = Badge.builder()
                .id(2L)
                .badgeType(BadgeType.LEVEL_REACHED)
                .threshold(5)
                .build();

        when(badgeRepo.findAll()).thenReturn(List.of(levelBadge));
        when(achievementRepo.existsByLearnerIdAndBadgeId("OMAR-123", 2L)).thenReturn(false);
        when(gamificationService.getLearnerLevel("OMAR-123")).thenReturn(5); // Niveau atteint !

        // Act
        badgeService.processBadgeAttribution(history);

        // Assert
        verify(achievementRepo).save(any(UserAchievement.class));
    }

    @Test
    void getBadgeById_ShouldThrowException_WhenNotFound() {
        when(badgeRepo.findById(99L)).thenReturn(Optional.empty());

        try {
            badgeService.getBadgeById(99L);
        } catch (RuntimeException e) {
            assertThat(e.getMessage()).contains("Badge non trouvé");
        }
    }
}
