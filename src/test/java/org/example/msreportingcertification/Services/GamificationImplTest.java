package org.example.msreportingcertification.Services;


import org.example.msreportingcertification.entities.EvaluationHistory;
import org.example.msreportingcertification.entities.LearnerProgression;
import org.example.msreportingcertification.repositories.EvaluationHistoryRepository;
import org.example.msreportingcertification.repositories.LearnerProgressionRepository;
import org.example.msreportingcertification.services.impl.GamificationImpl;
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
class GamificationImplTest {

    @Mock
    private LearnerProgressionRepository progressionRepo;
    @Mock
    private EvaluationHistoryRepository historyRepo;

    @InjectMocks
    private GamificationImpl gamificationService;

    private EvaluationHistory history;
    private LearnerProgression progression;

    @BeforeEach
    void setUp() {
        history = new EvaluationHistory();
        history.setLearnerId("USER_TEST");
        history.setLearnerName("Omar");
        history.setPercentage(80.0); // Devrait donner 80 + 50 = 130 XP

        progression = LearnerProgression.builder()
                .learnerId("USER_TEST")
                .totalXp(400L)
                .currentLevel(1)
                .totalCertificates(0)
                .build();
    }

    @Test
    void processGamification_ShouldIncreaseXpAndLevelUp() {
        // Arrange : L'utilisateur a 400 XP, on ajoute 130 XP -> Total 530 -> Niveau 2
        when(progressionRepo.findById("USER_TEST")).thenReturn(Optional.of(progression));

        // Act
        gamificationService.processGamification(history);

        // Assert
        assertThat(progression.getTotalXp()).isEqualTo(530L);
        assertThat(progression.getCurrentLevel()).isEqualTo(2);
        assertThat(progression.getTotalCertificates()).isEqualTo(1);

        verify(historyRepo).save(history);
        verify(progressionRepo).save(progression);
    }

    @Test
    void getLearnerLevel_ShouldReturnCorrectLevelBasedOnXp() {
        // Test de la formule (XP / 500) + 1
        when(progressionRepo.findTotalXpByLearnerId("USER_TEST")).thenReturn(1200L);

        Integer level = gamificationService.getLearnerLevel("USER_TEST");

        // (1200 / 500) = 2 -> 2 + 1 = 3
        assertThat(level).isEqualTo(3);
    }



    @Test
    void processGamification_ShouldCreateNewProgression_WhenFirstTime() {
        // Arrange : L'utilisateur n'existe pas encore en base
        when(progressionRepo.findById("NEW_USER")).thenReturn(Optional.empty());
        history.setLearnerId("NEW_USER");

        // Act
        gamificationService.processGamification(history);

        // Assert
        verify(progressionRepo).save(argThat(p ->
                p.getLearnerId().equals("NEW_USER") && p.getTotalXp() == 130L
        ));
    }
}
