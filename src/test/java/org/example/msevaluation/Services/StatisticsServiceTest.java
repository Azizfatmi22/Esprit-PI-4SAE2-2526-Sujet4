package org.example.msevaluation.Services;

import org.example.msevaluation.dto.QuestionDifficultyDTO;
import org.example.msevaluation.dto.ScoreDistributionDTO;
import org.example.msevaluation.entities.Evaluation;
import org.example.msevaluation.entities.QuizzQuestions;
import org.example.msevaluation.entities.TypeAssessment;
import org.example.msevaluation.repositories.EvaluationRepository;
import org.example.msevaluation.repositories.LearnerExamAnswerRepository;
import org.example.msevaluation.repositories.LearnerQuizzAnswerRepository;
import org.example.msevaluation.services.impl.StatisticsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatisticsServiceTest {

    @Mock
    private LearnerQuizzAnswerRepository quizAnswerRepository;
    @Mock
    private LearnerExamAnswerRepository examAnswerRepository;
    @Mock
    private EvaluationRepository evaluationRepository;

    @InjectMocks
    private StatisticsService statisticsService;

    private Evaluation quizEval;

    @BeforeEach
    void setUp() {
        quizEval = new Evaluation();
        quizEval.setId(1L);
        quizEval.setTypeAssessment(TypeAssessment.QUIZ);

        QuizzQuestions q1 = new QuizzQuestions();
        q1.setId(10L);
        q1.setQuestion("What is Java?");

        quizEval.setQuizzQuestions(new ArrayList<>(List.of(q1)));
    }

    @Test
    void getQuestionsAnalysis_ShouldIdentifyHardQuestion() {
        // Arrange : 10 tentatives, seulement 2 corrects (20% -> HARD)
        when(evaluationRepository.findById(1L)).thenReturn(Optional.of(quizEval));
        when(quizAnswerRepository.countByQuestionId(10L)).thenReturn(10L);
        when(quizAnswerRepository.countByQuestionIdAndSelectedAnswerIsCorrectTrue(10L)).thenReturn(2L);

        // Act
        List<QuestionDifficultyDTO> result = statisticsService.getQuestionsAnalysis(1L);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDifficultyLevel()).isEqualTo("HARD");
        assertThat(result.get(0).getSuccessRate()).isEqualTo(20.0);
    }

    @Test
    void getScoreDistribution_ShouldCategorizeScoresCorrectly() {
        // Arrange : Une liste de scores variés
        List<Double> scores = List.of(10.0, 15.0, 50.0, 95.0, 99.0);
        when(quizAnswerRepository.findAllFinalScoresByEvaluation(1L)).thenReturn(scores);

        // Act
        List<ScoreDistributionDTO> distribution = statisticsService.getScoreDistribution(1L, TypeAssessment.QUIZ);

        // Assert
        // Bucket 0-20% : doit contenir 10.0 et 15.0 (soit 2 entrées)
        assertThat(distribution.get(0).getRange()).isEqualTo("0-20%");
        assertThat(distribution.get(0).getCount()).isEqualTo(2);

        // Bucket 81-100% : doit contenir 95.0 et 99.0 (soit 2 entrées)
        assertThat(distribution.get(4).getCount()).isEqualTo(2);

        // Vérification du pourcentage (2/5 = 40%)
        assertThat(distribution.get(0).getPercentage()).isEqualTo(40.0);
    }
}
