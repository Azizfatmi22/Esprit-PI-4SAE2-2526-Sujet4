package org.example.msevaluation.repositories;


import org.example.msevaluation.entities.QuizzQuestions;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuizzQuestionsRepository extends JpaRepository<QuizzQuestions, Long> {
}
