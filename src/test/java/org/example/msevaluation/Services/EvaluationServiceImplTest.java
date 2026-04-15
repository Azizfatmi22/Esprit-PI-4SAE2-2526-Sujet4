package org.example.msevaluation.Services;

import org.example.msevaluation.entities.*;
import org.example.msevaluation.repositories.EvaluationRepository;
import org.example.msevaluation.services.impl.EvaluationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EvaluationServiceImplTest {

    @Mock
    private EvaluationRepository evaluationRepository;

    @InjectMocks
    private EvaluationServiceImpl evaluationService;

    private Evaluation sampleEval;

    @BeforeEach
    void setUp() {
        sampleEval = new Evaluation();
        sampleEval.setId(1L);
        sampleEval.setTitle("Java Unit Test");
        sampleEval.setDuration(60);
        sampleEval.setDate(LocalDateTime.now().minusMinutes(30));
    }

    @Test
    void isAccessAuthorized_ShouldReturnTrue_WhenTimeIsCorrect() {
        boolean result = evaluationService.isAccessAuthorized(sampleEval);
        assertTrue(result);
    }

    @Test
    void getEvaluationById_ShouldReturnEvaluation_WhenFound() {
        when(evaluationRepository.findById(1L)).thenReturn(Optional.of(sampleEval));

        Evaluation found = evaluationService.getEvaluationById(1L);

        assertThat(found.getTitle()).isEqualTo("Java Unit Test");
    }

    @Test
    void getEvaluationById_ShouldThrowException_WhenNotFound() {
        when(evaluationRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> evaluationService.getEvaluationById(99L));
    }

    @Test
    void saveEvaluation_ShouldSetBidirectionalLinks() {
        // Arrange
        QuizzQuestions q = new QuizzQuestions();
        sampleEval.setQuizzQuestions(new ArrayList<>(List.of(q)));
        when(evaluationRepository.save(any(Evaluation.class))).thenReturn(sampleEval);

        // Act
        Evaluation saved = evaluationService.saveEvaluation(sampleEval);

        // Assert
        assertNotNull(q.getEvaluation());
        verify(evaluationRepository).save(sampleEval);
    }

    @Test
    void updateEvaluation_ShouldHandleQuizType() {
        // Arrange
        Evaluation existing = new Evaluation();
        existing.setId(1L);
        existing.setQuizzQuestions(new ArrayList<>());
        existing.setExamQuestions(new ArrayList<>());

        Evaluation updateData = new Evaluation();
        updateData.setTypeAssessment(TypeAssessment.QUIZ);
        updateData.setQuizzQuestions(new ArrayList<>(List.of(new QuizzQuestions())));

        when(evaluationRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(evaluationRepository.save(any(Evaluation.class))).thenAnswer(i -> i.getArguments()[0]);

        // Act
        Evaluation result = evaluationService.updateEvaluation(1L, updateData);

        // Assert
        assertEquals(TypeAssessment.QUIZ, result.getTypeAssessment());
        assertEquals(1, result.getQuizzQuestions().size());
    }

    @Test
    void getGlobalSuccessRate_ShouldCalculateCorrectly() {
        // Arrange
        when(evaluationRepository.calculateAvgSuccessRateExams()).thenReturn(80.0);
        when(evaluationRepository.countCorrectQuizAnswers()).thenReturn(10L);
        when(evaluationRepository.countTotalQuizAnswers()).thenReturn(20L); // 50%

        // Act
        Double globalRate = evaluationService.getGlobalSuccessRate();

        // Assert (80 + 50) / 2 = 65.0
        assertEquals(65.0, globalRate);
    }

    @Test
    void deleteEvaluation_ShouldCallRepository() {
        evaluationService.deleteEvaluation(1L);
        verify(evaluationRepository, times(1)).deleteById(1L);
    }
}