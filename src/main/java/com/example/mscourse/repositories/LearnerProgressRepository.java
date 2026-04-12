package com.example.mscourse.repositories;

import com.example.mscourse.entities.LearnerProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LearnerProgressRepository extends JpaRepository<LearnerProgress, Long> {
    
    Optional<LearnerProgress> findByLearnerIdAndCourseId(String learnerId, Long courseId);
    
    List<LearnerProgress> findByLearnerId(String learnerId);
    
    List<LearnerProgress> findByCourseId(Long courseId);
    
    List<LearnerProgress> findByLearnerIdAndIsCompleted(String learnerId, Boolean isCompleted);
    
    Long countByLearnerIdAndIsCompleted(String learnerId, Boolean isCompleted);
}
