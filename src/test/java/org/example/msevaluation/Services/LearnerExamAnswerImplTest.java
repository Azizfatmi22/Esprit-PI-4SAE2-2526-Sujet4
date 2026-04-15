package org.example.msevaluation.Services;

import org.example.msevaluation.dto.EvaluationResultDTO;
import org.example.msevaluation.entities.Evaluation;
import org.example.msevaluation.entities.ExamQuestions;
import org.example.msevaluation.entities.LearnerExamAnswer;
import org.example.msevaluation.repositories.ExamQuestionsRepository;
import org.example.msevaluation.repositories.LearnerExamAnswerRepository;
import org.example.msevaluation.services.impl.LearnerExamAnswerImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LearnerExamAnswerImplTest {

    @Mock
    private LearnerExamAnswerRepository learnerExamAnswerRepository;

    @Mock
    private ExamQuestionsRepository examQuestionsRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private LearnerExamAnswerImpl learnerExamAnswerService;

    private List<LearnerExamAnswer> answers;
    private ExamQuestions question;
    private Evaluation evaluation;

    @BeforeEach
    void setUp() {
        evaluation = new Evaluation();
        evaluation.setId(1L);
        evaluation.setTitle("Java Certification");
        evaluation.setDuration(60);
        evaluation.setMinSuccessScore(50);
        evaluation.setDate(LocalDateTime.now().minusMinutes(20));

        question = new ExamQuestions();
        question.setId(10L);
        question.setPoints(10);
        question.setKeywords(List.of("Java", "Spring"));
        question.setEvaluation(evaluation);

        LearnerExamAnswer answer = new LearnerExamAnswer();
        answer.setLearnerId("OMAR123");
        answer.setLearnerName("Omar Aguil");
        answer.setAnswerOfLearner("Java and Spring are great");
        answer.setQuestion(question);
        answer.setTimeSpentSeconds(300);

        answers = new ArrayList<>(List.of(answer));
    }

    @Test
    void saveAllAnswers_ShouldCalculateScoreAndSendToRabbit() {
        // Arrange
        when(examQuestionsRepository.findAllByIdIn(anyList())).thenReturn(List.of(question));

        // Act
        EvaluationResultDTO result = learnerExamAnswerService.saveAllAnswers(answers);

        // Assert
        assertThat(result.getScoreObtained()).isEqualTo(10.0); // Les 2 mots clés sont présents
        assertThat(result.getPercentage()).isEqualTo(100.0);
        assertThat(result.getIsPassed()).isTrue();
        assertThat(result.getLearnerName()).isEqualTo("Omar Aguil");

        // Vérifie que les données sont sauvegardées
        verify(learnerExamAnswerRepository, times(1)).saveAll(anyList());

        // Vérifie que le message est envoyé à RabbitMQ
        verify(rabbitTemplate, times(1)).convertAndSend(any(), any(), eq(result));
    }

    @Test
    void calculateAutomaticScore_ShouldReturnZero_WhenKeywordsMissing() {
        // Simulation d'une réponse incomplète
        answers.get(0).setAnswerOfLearner("Only Java is here"); // Manque "Spring"
        when(examQuestionsRepository.findAllByIdIn(anyList())).thenReturn(List.of(question));

        EvaluationResultDTO result = learnerExamAnswerService.saveAllAnswers(answers);

        assertThat(result.getScoreObtained()).isEqualTo(0.0);
        assertThat(result.getIsPassed()).isFalse();
    }

    @Test
    void isSubmittingTooFast_ShouldReturnTrue_WhenTimeIsTooShort() {
        // Si l'examen a commencé il y a seulement 10 secondes pour une durée de 60 min
        LocalDateTime start = LocalDateTime.now().minusSeconds(10);
        boolean suspicious = learnerExamAnswerService.isSubmittingTooFast(start, 60);

        assertThat(suspicious).isTrue();
    }
}