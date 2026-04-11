package org.example.msreportingcertification.repositories;


import org.example.msreportingcertification.entities.LearnerProgression;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LearnerProgressionRepository extends JpaRepository<LearnerProgression, String> {
    List<LearnerProgression> findTop10ByOrderByTotalXpDesc();
    @Query("SELECT p.totalXp FROM LearnerProgression p WHERE p.learnerId = :learnerId")
    Long findTotalXpByLearnerId(@Param("learnerId") String learnerId);
}
