package org.example.msevaluation.repositories;

import org.example.msevaluation.entities.LearnerExamAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LearnerExamAnswerRepository extends JpaRepository<LearnerExamAnswer, Long> {
    List<LearnerExamAnswer> findByLearnerId(Long learnerId);
    long countByQuestionId(Long questionId);
    long countByQuestionIdAndScoreGreaterThan(Long questionId, Double score);

    @Query("SELECT (SUM(lea.score) / SUM(q.points) * 100) " +
            "FROM LearnerExamAnswer lea " +
            "JOIN lea.question q " +
            "WHERE q.evaluation.id = :evalId " +
            "GROUP BY lea.learnerId")
    List<Double> findAllFinalScoresByEvaluation(@Param("evalId") Long evalId);
}
