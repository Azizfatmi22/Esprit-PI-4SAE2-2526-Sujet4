package com.mspathandschedule.repositories;

import com.mspathandschedule.entities.LearningPath;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LearningPathRepository extends JpaRepository<LearningPath, Long> {

}