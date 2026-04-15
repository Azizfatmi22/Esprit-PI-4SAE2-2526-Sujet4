package org.example.msevaluation.Services;
import org.example.msevaluation.dto.EvaluationResultDTO;
import org.example.msevaluation.entities.*;
import org.example.msevaluation.repositories.LearnerQuizzAnswerRepository;
import org.example.msevaluation.repositories.QuizzAnswerRepository;
import org.example.msevaluation.repositories.QuizzQuestionsRepository;
import org.example.msevaluation.services.impl.LearnerQuizzAnswerImpl;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LearnerQuizzAnswerImplTest {

    @Mock
    private LearnerQuizzAnswerRepository repository;
    @Mock
    private QuizzAnswerRepository quizzAnswerRepository;
    @Mock
    private QuizzQuestionsRepository quizzQuestionsRepository;
    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private LearnerQuizzAnswerImpl learnerQuizzAnswerService;

    private Evaluation evaluation;
    private QuizzQuestions question;
    private QuizzAnswer selectedAnswer;
    private List<LearnerQuizzAnswer> answers;

    @BeforeEach
    void setUp() {
        evaluation = new Evaluation();
        evaluation.setId(1L);
        evaluation.setTitle("Java Quiz");
        evaluation.setDuration(30);
        evaluation.setMinSuccessScore(50);
        evaluation.setDate(LocalDateTime.now().minusMinutes(10));

        question = new QuizzQuestions();
        question.setId(100L);
        question.setPoints(5);
        question.setEvaluation(evaluation);

        selectedAnswer = new QuizzAnswer();
        selectedAnswer.setId(500L);

        LearnerQuizzAnswer userAns = new LearnerQuizzAnswer();
        userAns.setLearnerId("OMAR123");
        userAns.setLearnerName("Omar Aguil");
        userAns.setQuestion(question);
        userAns.setSelectedAnswer(selectedAnswer);
        userAns.setTimeSpentSeconds(120);

        answers = new ArrayList<>(List.of(userAns));
    }

    @Test
    void saveAllQuizAnswers_ShouldCalculateCorrectScoreAndSendToRabbit() {
        // Arrange
        // On mocke la récupération de l'évaluation via la question
        when(quizzQuestionsRepository.findById(100L)).thenReturn(Optional.of(question));
        // On mocke le fait que l'ID de la réponse sélectionnée est dans la liste des "correctes"
        when(quizzAnswerRepository.findCorrectAnswerIdsIn(anyList())).thenReturn(List.of(500L));

        // Act
        EvaluationResultDTO result = learnerQuizzAnswerService.saveAllQuizAnswers(answers);

        // Assert
        assertThat(result.getScoreObtained()).isEqualTo(5.0);
        assertThat(result.getPercentage()).isEqualTo(100.0);
        assertThat(result.getIsPassed()).isTrue();
        assertThat(result.getType()).isEqualTo("QUIZ");

        // Vérification des interactions
        verify(repository, times(1)).saveAll(anyList());
        verify(rabbitTemplate, times(1)).convertAndSend(any(), any(), eq(result));
    }

    @Test
    void saveAllQuizAnswers_ShouldMarkAsSuspicious_WhenTimeTooFast() {
        // Arrange: Examen commencé il y a 2 secondes seulement
        evaluation.setDate(LocalDateTime.now().minusSeconds(2));
        when(quizzQuestionsRepository.findById(100L)).thenReturn(Optional.of(question));
        when(quizzAnswerRepository.findCorrectAnswerIdsIn(anyList())).thenReturn(new ArrayList<>());

        // Act
        EvaluationResultDTO result = learnerQuizzAnswerService.saveAllQuizAnswers(answers);

        // Assert
        assertThat(result.getIsSuspicious()).isTrue();
    }
}
