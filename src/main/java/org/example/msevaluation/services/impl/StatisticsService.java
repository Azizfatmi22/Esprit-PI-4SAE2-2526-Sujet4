package org.example.msevaluation.services.impl;

import lombok.RequiredArgsConstructor;
import org.example.msevaluation.dto.QuestionDifficultyDTO;
import org.example.msevaluation.dto.ScoreDistributionDTO;
import org.example.msevaluation.entities.Evaluation;
import org.example.msevaluation.entities.TypeAssessment;
import org.example.msevaluation.repositories.EvaluationRepository;
import org.example.msevaluation.repositories.LearnerExamAnswerRepository;
import org.example.msevaluation.repositories.LearnerQuizzAnswerRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final LearnerQuizzAnswerRepository quizAnswerRepository;
    private final LearnerExamAnswerRepository examAnswerRepository;
    private final EvaluationRepository evaluationRepository;



    public List<QuestionDifficultyDTO> getQuestionsAnalysis(Long evaluationId) {
        Evaluation eval = evaluationRepository.findById(evaluationId)
                .orElseThrow(() -> new RuntimeException("Evaluation non trouvée"));

        List<QuestionDifficultyDTO> analysis = new ArrayList<>();

        if (eval.getTypeAssessment() == TypeAssessment.QUIZ) {
            eval.getQuizzQuestions().forEach(q -> {
                long total = quizAnswerRepository.countByQuestionId(q.getId());
                long correct = quizAnswerRepository.countByQuestionIdAndSelectedAnswerIsCorrectTrue(q.getId());
                analysis.add(buildDTO(q.getId(), q.getQuestion(), correct, total));
            });
        } else {
            eval.getExamQuestions().forEach(q -> {
                long total = examAnswerRepository.countByQuestionId(q.getId());
                long correct = examAnswerRepository.countByQuestionIdAndScoreGreaterThan(q.getId(), 0.0);
                analysis.add(buildDTO(q.getId(), q.getQuestion(), correct, total));
            });
        }
        return analysis;
    }

    private QuestionDifficultyDTO buildDTO(Long id, String text, long correct, long total) {
        double rate = (total > 0) ? ((double) correct / total) * 100 : 0.0;
        String level = rate < 30 ? "HARD" : (rate < 70 ? "MEDIUM" : "EASY");

        return QuestionDifficultyDTO.builder()
                .questionId(id)
                .questionText(text)
                .successRate(Math.round(rate * 100.0) / 100.0)
                .totalAttempts(total)
                .difficultyLevel(level)
                .build();
    }


    public List<ScoreDistributionDTO> getScoreDistribution(Long evaluationId, TypeAssessment type) {
        List<Double> scores = (type == TypeAssessment.QUIZ)
                ? quizAnswerRepository.findAllFinalScoresByEvaluation(evaluationId)
                : examAnswerRepository.findAllFinalScoresByEvaluation(evaluationId);

        long[] buckets = new long[5]; // [0-20, 21-40, 41-60, 61-80, 81-100]

        for (Double score : scores) {
            if (score <= 20) buckets[0]++;
            else if (score <= 40) buckets[1]++;
            else if (score <= 60) buckets[2]++;
            else if (score <= 80) buckets[3]++;
            else buckets[4]++;
        }

        return Arrays.asList(
                new ScoreDistributionDTO("0-20%", buckets[0], calculatePercent(buckets[0], scores.size())),
                new ScoreDistributionDTO("21-40%", buckets[1], calculatePercent(buckets[1], scores.size())),
                new ScoreDistributionDTO("41-60%", buckets[2], calculatePercent(buckets[2], scores.size())),
                new ScoreDistributionDTO("61-80%", buckets[3], calculatePercent(buckets[3], scores.size())),
                new ScoreDistributionDTO("81-100%", buckets[4], calculatePercent(buckets[4], scores.size()))
        );
    }

    private double calculatePercent(long count, int total) {
        return total > 0 ? (double) count / total * 100 : 0;
    }
}