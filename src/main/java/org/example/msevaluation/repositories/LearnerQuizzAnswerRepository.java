package org.example.msevaluation.repositories;


import org.example.msevaluation.entities.LearnerQuizzAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LearnerQuizzAnswerRepository extends JpaRepository<LearnerQuizzAnswer, Long> {
    List<LearnerQuizzAnswer> findByLearnerIdAndQuestionEvaluationId(Long learnerId, Long evaluationId);
    long countByQuestionId(Long questionId);
    long countByQuestionIdAndSelectedAnswerIsCorrectTrue(Long questionId);

    @Query("SELECT (CAST(COUNT(CASE WHEN qa.isCorrect = true THEN 1 END) AS double) / COUNT(lqa) * 100) " +
            "FROM LearnerQuizzAnswer lqa " +
            "JOIN lqa.selectedAnswer qa " +
            "WHERE lqa.question.evaluation.id = :evalId " +
            "GROUP BY lqa.learnerId")
    List<Double> findAllFinalScoresByEvaluation(@Param("evalId") Long evalId);
}
