package com.example.mscourse.services.interfaces;

import com.example.mscourse.dto.*;
import com.example.mscourse.entities.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface ICourseService {

    // Create operations
    CourseDTO createCourse(CreateCourseRequestDTO courseDTO);

    // Read operations
    CourseDTO getCourseById(Long id);
    Page<CourseSummaryDTO> getAllCourses(Pageable pageable);
    List<CourseSummaryDTO> getCoursesByLevel(Level level);
    List<CourseSummaryDTO> getCoursesByTrainer(Long trainerId);
    List<CourseSummaryDTO> searchCourses(String keyword);

    // Update operations
    CourseDTO updateCourse(Long id, UpdateCourseRequestDTO courseDTO);
    CourseDTO updateCourseStatus(Long id, String status);
    CourseDTO updateCourseRating(Long id, Double rating);

    // Delete operations
    void deleteCourse(Long id);

    // Chapter operations
    ChapterDTO addChapterToCourse(Long courseId, ChapterDTO chapterDTO);
    List<ChapterDTO> getCourseChapters(Long courseId);
}