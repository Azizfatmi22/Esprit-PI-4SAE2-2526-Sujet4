package com.example.mscourse.repositories;

import com.example.mscourse.entities.Chapter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ChapterRepository extends JpaRepository<Chapter, Long> {

    // Find all chapters of a course
    List<Chapter> findByCourseId(Long courseId);

    // Delete all chapters of a course
    void deleteByCourseId(Long courseId);

    // Check if course has chapters
    boolean existsByCourseId(Long courseId);
}