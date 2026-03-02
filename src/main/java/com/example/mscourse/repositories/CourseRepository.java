package com.example.mscourse.repositories;

import com.example.mscourse.entities.Course;
import com.example.mscourse.entities.Level;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {

    // Find courses by trainer
    List<Course> findByTrainerId(String trainerId);

    // Find courses by level
    List<Course> findByLevel(Level level);

    // Find courses by status
    List<Course> findByStatus(String status);

    // Find courses by trainer and status
    List<Course> findByTrainerIdAndStatus(String trainerId, String status);

    // Search courses by title or description (case insensitive)
    @Query("SELECT c FROM Course c WHERE LOWER(c.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(c.description) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Course> searchCourses(@Param("keyword") String keyword);

    // Find published courses with pagination
    Page<Course> findByStatus(String status, Pageable pageable);

    // Find courses by level and status
    Page<Course> findByLevelAndStatus(Level level, String status, Pageable pageable);

    // Find courses with rating above threshold
    List<Course> findByRatingGreaterThanEqual(Double rating);

    // Find free courses (price = 0)
    List<Course> findByPriceLessThanEqual(Double price);

    // Count courses by trainer
    Long countByTrainerId(String trainerId);

    // Check if course exists with same title for trainer
    boolean existsByTitleAndTrainerId(String title, String trainerId);

    // Find latest courses
    List<Course> findTop10ByOrderByCreatedAtDesc();

    // Custom query to get course with chapters (for eager loading when needed)
    @Query("SELECT c FROM Course c LEFT JOIN FETCH c.chapters WHERE c.id = :id")
    Optional<Course> findCourseWithChapters(@Param("id") Long id);

    // Get course statistics
    @Query("SELECT COUNT(c) FROM Course c WHERE c.trainerId = :trainerId")
    long getTotalCoursesByTrainer(@Param("trainerId") String trainerId);

    @Query("SELECT AVG(c.rating) FROM Course c WHERE c.trainerId = :trainerId")
    Double getAverageRatingByTrainer(@Param("trainerId") String trainerId);
}