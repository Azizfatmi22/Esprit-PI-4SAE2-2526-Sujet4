package com.example.mscourse.services.interfaces;


import com.example.mscourse.dto.*;
import com.example.mscourse.entities.Level;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ICourseService {

    // Create operations
    CourseDTO createCourse(CreateCourseRequestDTO courseDTO);

    // Read operations
    CourseDTO getCourseById(Long id);
    CourseDTO getCourseWithChapters(Long id);
    Page<CourseSummaryDTO> getAllCourses(Pageable pageable);
    List<CourseSummaryDTO> getCoursesByLevel(Level level);
    List<CourseSummaryDTO> getCoursesByTrainer(String trainerId);
    List<CourseSummaryDTO> searchCourses(String keyword);

    // Update operations
    CourseDTO updateCourse(Long id, UpdateCourseRequestDTO courseDTO);
    CourseDTO updateCourseStatus(Long id, String status);
    CourseDTO updateCourseRating(Long id, Double rating);
    CourseDTO updateCourseThumbnail(Long id, String thumbnailUrl);

    // Delete operations
    void deleteCourse(Long id);

    // Statistics
    CourseStatisticsDTO getCourseStatistics(String trainerId);
    List<CourseSummaryDTO> getTopRatedCourses(int limit);
    List<CourseSummaryDTO> getMostEnrolledCourses(int limit);

    // Validation
    boolean existsByTitleAndTrainer(String title, String trainerId);
}
