package org.example.msevaluation.repositories;

import org.example.msevaluation.entities.QuizzAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface QuizzAnswerRepository extends JpaRepository<QuizzAnswer, Long> {
    @Query("SELECT a.id FROM QuizzAnswer a WHERE a.id IN :ids AND a.isCorrect = true")
    List<Long> findCorrectAnswerIdsIn(@Param("ids") List<Long> ids);
}
