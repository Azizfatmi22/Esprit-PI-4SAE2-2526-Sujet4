package com.example.mscourse.repositories;

import com.example.mscourse.entities.Chapter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChapterRepository extends JpaRepository<Chapter, Long> {

    // Find chapters by course
    List<Chapter> findByCourseId(Long courseId);

    // Find chapters by course ordered by orderIndex
    List<Chapter> findByCourseIdOrderByOrderIndexAsc(Long courseId);

    // Find chapter with its content blocks
    @Query("SELECT c FROM Chapter c LEFT JOIN FETCH c.contentBlocks WHERE c.id = :id")
    Optional<Chapter> findChapterWithContentBlocks(@Param("id") Long id);

    // Find chapters by course with content blocks
    @Query("SELECT c FROM Chapter c LEFT JOIN FETCH c.contentBlocks WHERE c.course.id = :courseId ORDER BY c.orderIndex ASC")
    List<Chapter> findByCourseIdWithContentBlocks(@Param("courseId") Long courseId);

    // Get max order index for chapters in a course
    @Query("SELECT COALESCE(MAX(c.orderIndex), 0) FROM Chapter c WHERE c.course.id = :courseId")
    Integer getMaxOrderIndex(@Param("courseId") Long courseId);

    // Check if chapter exists in course
    boolean existsByIdAndCourseId(Long id, Long courseId);

    // Delete all chapters of a course
    @Modifying
    @Transactional
    @Query("DELETE FROM Chapter c WHERE c.course.id = :courseId")
    void deleteByCourseId(@Param("courseId") Long courseId);

    // Count chapters in a course
    Long countByCourseId(Long courseId);

    // Find chapter by course and order index
    Optional<Chapter> findByCourseIdAndOrderIndex(Long courseId, Integer orderIndex);

    // Reorder chapters (shift order indices)
    @Modifying
    @Transactional
    @Query("UPDATE Chapter c SET c.orderIndex = c.orderIndex + :shift WHERE c.course.id = :courseId AND c.orderIndex >= :fromIndex")
    void shiftOrderIndices(@Param("courseId") Long courseId, @Param("fromIndex") Integer fromIndex, @Param("shift") Integer shift);
}