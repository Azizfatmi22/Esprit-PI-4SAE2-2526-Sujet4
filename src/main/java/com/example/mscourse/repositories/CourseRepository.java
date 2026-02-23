package com.example.mscourse.repositories;

import com.example.mscourse.entities.Course;
import com.example.mscourse.entities.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {

    // Find course by exact title
    Optional<Course> findByTitle(String title);

    // Find all courses by level
    List<Course> findByLevel(Level level);

    // Find all courses by trainer
    List<Course> findByTrainerId(Long trainerId);

    // Find all courses by status
    List<Course> findByStatus(String status);

    // Custom query to search courses by title or description
    @Query("SELECT c FROM Course c WHERE " +
            "LOWER(c.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(c.description) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Course> searchCourses(@Param("keyword") String keyword);

    // Find courses by price range
    @Query("SELECT c FROM Course c WHERE c.price BETWEEN :minPrice AND :maxPrice")
    List<Course> findByPriceRange(@Param("minPrice") Double minPrice,
                                        @Param("maxPrice") Double maxPrice);

    // Find courses with rating above certain value
    List<Course> findByRatingGreaterThanEqual(Double rating);

    // Count courses by trainer
    Long countByTrainerId(Long trainerId);
}