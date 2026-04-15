package org.example.msevaluation.services.impl;

import lombok.RequiredArgsConstructor;
import org.example.msevaluation.configuration.RabbitMQConfig;
import org.example.msevaluation.dto.EvaluationResultDTO;
import org.example.msevaluation.entities.Evaluation;
import org.example.msevaluation.entities.ExamQuestions;
import org.example.msevaluation.entities.LearnerExamAnswer;
import org.example.msevaluation.repositories.ExamQuestionsRepository;
import org.example.msevaluation.repositories.LearnerExamAnswerRepository;
import org.example.msevaluation.services.interfaces.ILearnerExamAnswerService;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class LearnerExamAnswerImpl implements ILearnerExamAnswerService {

    private final LearnerExamAnswerRepository learnerExamAnswerRepository;
    private final ExamQuestionsRepository examQuestionsRepository;
    private final RabbitTemplate rabbitTemplate;

    @Override
    @Transactional
    public EvaluationResultDTO saveAllAnswers(List<LearnerExamAnswer> answers) {
        if (answers == null || answers.isEmpty()) {
            return buildEmptyResult();
        }
        String realLearnerId = answers.get(0).getLearnerId();
        String realLearnerName = answers.get(0).getLearnerName();

        Integer timeSpent = answers.get(0).getTimeSpentSeconds();
        List<Long> questionIds = answers.stream()
                .map(a -> a.getQuestion().getId())
                .toList();

        List<ExamQuestions> questionsFromDb = examQuestionsRepository.findAllByIdIn(questionIds);
        Evaluation eval = questionsFromDb.get(0).getEvaluation();
        boolean suspicious = isSubmittingTooFast(eval.getDate(), eval.getDuration());

        double earnedPoints = 0.0;
        double totalPoints = 0.0;
        int correctCount = 0;
        int totalQuestions = answers.size();
        LocalDateTime submissionTime = LocalDateTime.now();

        for (LearnerExamAnswer answer : answers) {
            ExamQuestions question = questionsFromDb.stream()
                    .filter(q -> q.getId().equals(answer.getQuestion().getId()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Question non trouvée : " + answer.getQuestion().getId()));

            double score = calculateAutomaticScore(answer.getAnswerOfLearner(), question);

            // Mise à jour de l'entité
            answer.setScore(score);
            answer.setLearnerId(realLearnerId);
            answer.setLearnerName(realLearnerName);
            answer.setQuestion(question);
            answer.setIsSuspicious(suspicious); // Stockage du flag d'audit
            answer.setResponseDate(submissionTime); // Date de fin réelle
            earnedPoints += score;
            totalPoints += question.getPoints();
            if (score > 0) correctCount++;
        }


        learnerExamAnswerRepository.saveAll(answers);

        double percentage = (totalPoints > 0) ? (earnedPoints / totalPoints) * 100 : 0.0;
        EvaluationResultDTO result = EvaluationResultDTO.builder()
                .learnerName(realLearnerName)
                .learnerId(realLearnerId)
                .evaluationTitle(eval.getTitle())
                .type("EXAM")
                .duration(eval.getDuration())
                .scoreObtained(earnedPoints)
                .totalPossiblePoints(totalPoints)
                .correctAnswersCount(correctCount)
                .wrongAnswersCount(totalQuestions - correctCount)
                .totalQuestions(totalQuestions)
                .percentage(Math.round(percentage * 100.0) / 100.0)
                .minSuccessScore(eval.getMinSuccessScore())
                .isPassed(percentage >= eval.getMinSuccessScore())
                .evaluationId(eval.getId())
                .timeSpentSeconds(timeSpent)
                .isSuspicious(suspicious)
                .build();
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE,
                    RabbitMQConfig.ROUTING_KEY,
                    result
            );
            System.out.println("Message envoyé à RabbitMQ pour l'évaluation : " + result.getEvaluationTitle());
        } catch (Exception e) {
            System.err.println("Erreur lors de l'envoi RabbitMQ : " + e.getMessage());
        }
        return  result;
    }

    private double calculateAutomaticScore(String studentAnswer, ExamQuestions question) {
        if (studentAnswer == null || studentAnswer.trim().isEmpty() ||
                question.getKeywords() == null || question.getKeywords().isEmpty()) {
            return 0.0;
        }

        String cleanStudentAnswer = studentAnswer.toLowerCase().trim();
        List<String> requiredKeywords = question.getKeywords();

        long foundCount = requiredKeywords.stream()
                .filter(keyword -> cleanStudentAnswer.contains(keyword.toLowerCase().trim()))
                .count();

        // Notation binaire basée sur la présence de tous les mots-clés
        if (foundCount == requiredKeywords.size()) {
            return question.getPoints();
        }

        return 0.0;
    }

    @Override
    public boolean isSubmittingTooFast(LocalDateTime scheduledDate, int durationInMinutes) {
        if (scheduledDate == null) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now();
        long secondsSinceStart = java.time.Duration.between(scheduledDate, now).getSeconds();
        long minimumSecondsRequired = (durationInMinutes * 60L) / 10;

        System.out.println("--- AUDIT TEMPOREL ---");
        System.out.println("Début officiel : " + scheduledDate);
        System.out.println("Soumission à   : " + now);
        System.out.println("Écart réel     : " + secondsSinceStart + " secondes");
        System.out.println("Seuil minimum  : " + minimumSecondsRequired + " secondes");

        return secondsSinceStart < minimumSecondsRequired;
    }

    private EvaluationResultDTO buildEmptyResult() {
        return EvaluationResultDTO.builder()
                .learnerName("Omar Aguil")
                .evaluationTitle("Examen vide")
                .type("EXAM")
                .scoreObtained(0.0)
                .isPassed(false)
                .isSuspicious(false)
                .build();
    }

    @Override
    public List<LearnerExamAnswer> getAnswersByLearnerId(Long learnerId) {
        return learnerExamAnswerRepository.findByLearnerId(learnerId);
    }
}