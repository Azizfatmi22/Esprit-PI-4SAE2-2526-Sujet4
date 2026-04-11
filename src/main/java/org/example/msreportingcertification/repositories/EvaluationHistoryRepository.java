package org.example.msreportingcertification.repositories;

import org.example.msreportingcertification.entities.EvaluationHistory;
import org.example.msreportingcertification.entities.VigilanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EvaluationHistoryRepository extends JpaRepository<EvaluationHistory, Long> {

    List<EvaluationHistory> findByEvaluationId(Long evaluationId);
    Optional<EvaluationHistory> findFirstByLearnerIdAndEvaluationIdOrderByReceivedAtDesc(String learnerId, Long evaluationId);
    List<EvaluationHistory> findByLearnerId(String learnerId);
    Optional<EvaluationHistory> findFirstByLearnerIdOrderByReceivedAtDesc(String learnerId);
    long countByLearnerIdAndIsPassedAndVigilanceStatus(String learnerId, Boolean isPassed, VigilanceStatus vigilanceStatus);
    @Query("SELECT SUM(e.pointsGained) FROM EvaluationHistory e WHERE e.learnerId = :learnerId AND e.vigilanceStatus = :status")
    Long sumPointsByLearnerIdAndVigilanceStatus(@Param("learnerId") String learnerId, @Param("status") VigilanceStatus status);

}
