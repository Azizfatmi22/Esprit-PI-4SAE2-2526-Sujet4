package org.example.msevaluation.repositories;


import org.example.msevaluation.entities.ExamQuestions;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ExamQuestionsRepository extends JpaRepository<ExamQuestions, Long> {
    @Query("SELECT q FROM ExamQuestions q WHERE q.id IN :ids")
    List<ExamQuestions> findAllByIdIn(List<Long> ids);
}
