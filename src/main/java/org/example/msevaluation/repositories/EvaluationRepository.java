package org.example.msevaluation.repositories;

import org.example.msevaluation.entities.Evaluation;
import org.example.msevaluation.entities.TypeAssessment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EvaluationRepository extends JpaRepository<Evaluation, Long> {
    @Query("SELECT AVG(CASE WHEN lea.score >= e.minSuccessScore THEN 100.0 ELSE 0.0 END) " +
            "FROM Evaluation e JOIN e.examQuestions eq JOIN LearnerExamAnswer lea ON lea.question.id = eq.id")
    Double calculateAvgSuccessRateExams();

    @Query("SELECT COUNT(lqa) FROM LearnerQuizzAnswer lqa WHERE lqa.selectedAnswer.isCorrect = true")
    Long countCorrectQuizAnswers();

    @Query("SELECT COUNT(lqa) FROM LearnerQuizzAnswer lqa")
    Long countTotalQuizAnswers();

    long countByTypeAssessment(TypeAssessment typeAssessment);

    List<Evaluation> findByTrainerId(String trainerId);
}
