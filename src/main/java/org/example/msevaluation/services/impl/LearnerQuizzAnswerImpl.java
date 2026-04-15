package org.example.msevaluation.services.impl;

import lombok.RequiredArgsConstructor;
import org.example.msevaluation.configuration.RabbitMQConfig;
import org.example.msevaluation.dto.EvaluationResultDTO;
import org.example.msevaluation.entities.*;
import org.example.msevaluation.repositories.*;
import org.example.msevaluation.services.interfaces.ILearnerQuizzAnswerService;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LearnerQuizzAnswerImpl implements ILearnerQuizzAnswerService {
    private final LearnerQuizzAnswerRepository repository;
    private final QuizzAnswerRepository quizzAnswerRepository;
    private final QuizzQuestionsRepository quizzQuestionsRepository;
    private final RabbitTemplate rabbitTemplate;

    @Transactional
    @Override
    public EvaluationResultDTO saveAllQuizAnswers(List<LearnerQuizzAnswer> answers) {
        if (answers == null || answers.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La liste des réponses ne peut pas être vide.");
        }

        String realLearnerId = answers.get(0).getLearnerId();
        String realLearnerName = answers.get(0).getLearnerName();
        Integer timeSpent = answers.get(0).getTimeSpentSeconds();

        Evaluation eval = quizzQuestionsRepository.findById(answers.get(0).getQuestion().getId())
                .map(QuizzQuestions::getEvaluation)
                .orElseThrow(() -> new RuntimeException("Évaluation introuvable"));

        boolean isSuspicious = isSubmittingTooFast(eval.getDate(), eval.getDuration());

        double earnedPoints = 0.0;
        double totalPossiblePoints = 0.0;
        int correctCount = 0;
        LocalDateTime submissionTime = LocalDateTime.now();

        // Optimisation : Récupération groupée des IDs de réponses correctes
        List<Long> selectedIds = answers.stream()
                .map(ans -> ans.getSelectedAnswer().getId())
                .toList();
        List<Long> correctIds = quizzAnswerRepository.findCorrectAnswerIdsIn(selectedIds);

        for (LearnerQuizzAnswer ans : answers) {
            ans.setResponseDate(submissionTime);
            ans.setIsSuspicious(isSuspicious);

            QuizzQuestions question = quizzQuestionsRepository.findById(ans.getQuestion().getId())
                    .orElseThrow(() -> new RuntimeException("Question non trouvée"));

            totalPossiblePoints += question.getPoints();

            if (correctIds.contains(ans.getSelectedAnswer().getId())) {
                earnedPoints += question.getPoints();
                correctCount++;
            }
            ans.setQuestion(question);
        }


        repository.saveAll(answers);

        double percentage = (totalPossiblePoints > 0) ? (earnedPoints / totalPossiblePoints) * 100 : 0.0;
        boolean isPassed = percentage >= eval.getMinSuccessScore();

        EvaluationResultDTO result = EvaluationResultDTO.builder()
                .learnerName(realLearnerName)
                .learnerId(realLearnerId)
                .evaluationTitle(eval.getTitle())
                .type("QUIZ")
                .scoreObtained(earnedPoints)
                .totalPossiblePoints(totalPossiblePoints)
                .duration(eval.getDuration())
                .correctAnswersCount(correctCount)
                .wrongAnswersCount(answers.size() - correctCount)
                .totalQuestions(answers.size())
                .percentage(Math.round(percentage * 100.0) / 100.0)
                .minSuccessScore(eval.getMinSuccessScore())
                .isPassed(isPassed)
                .timeSpentSeconds(timeSpent)
                .evaluationId(eval.getId())
                .isSuspicious(isSuspicious)
                .build();


        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE,
                    RabbitMQConfig.ROUTING_KEY,
                    result
            );
            System.out.println("[QUIZ] Résultat envoyé à RabbitMQ : " + result.getEvaluationTitle());
        } catch (Exception e) {
            System.err.println(" Erreur RabbitMQ sur le Quiz : " + e.getMessage());
        }

        return result;
    }

    @Override
    public boolean isSubmittingTooFast(LocalDateTime scheduledStartTime, int durationInMinutes) {
        if (scheduledStartTime == null) return false;

        LocalDateTime now = LocalDateTime.now();
        long secondsSpent = java.time.Duration.between(scheduledStartTime, now).getSeconds();
        long minimumSecondsRequired = (durationInMinutes * 60L) / 10;
        System.out.println("--- AUDIT QUIZ ---");
        System.out.println("Début programmé : " + scheduledStartTime);
        System.out.println("Temps écoulé    : " + secondsSpent + "s");
        System.out.println("Seuil requis    : " + minimumSecondsRequired + "s");

        return secondsSpent < minimumSecondsRequired;
    }
}