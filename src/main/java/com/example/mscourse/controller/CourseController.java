package com.example.mscourse.controller;

import com.example.mscourse.dto.*;
import com.example.mscourse.entities.Level;
import com.example.mscourse.services.interfaces.ICourseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController // This class handles HTTP requests
@RequestMapping("/api/courses") // Base URL for all endpoints in this controller
@RequiredArgsConstructor
@Slf4j
public class CourseController {

    private final ICourseService courseService;
    @PostMapping
    public ResponseEntity<CourseDTO> createCourse(
            @Valid @RequestBody CreateCourseRequestDTO courseDTO) {
        log.info("REST request to create course: {}", courseDTO.getTitle());
        CourseDTO result = courseService.createCourse(courseDTO);
        return new ResponseEntity<>(result, HttpStatus.CREATED);
    }

    // GET /api/v1/courses/{id} - Get course by ID
    @GetMapping("/{id}")
    public ResponseEntity<CourseDTO> getCourseById(@PathVariable Long id) {
        log.info("REST request to get course by ID: {}", id);
        CourseDTO result = courseService.getCourseById(id);
        return ResponseEntity.ok(result);
    }

    // GET /api/v1/courses - Get all courses with pagination
    @GetMapping
    public ResponseEntity<Page<CourseSummaryDTO>> getAllCourses(
            @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC)
            Pageable pageable) {
        log.info("REST request to get all courses");
        Page<CourseSummaryDTO> result = courseService.getAllCourses(pageable);
        return ResponseEntity.ok(result);
    }

    // GET /api/v1/courses/level/{level} - Get courses by level
    @GetMapping("/level/{level}")
    public ResponseEntity<List<CourseSummaryDTO>> getCoursesByLevel(
            @PathVariable Level level) {
        log.info("REST request to get courses by level: {}", level);
        List<CourseSummaryDTO> result = courseService.getCoursesByLevel(level);
        return ResponseEntity.ok(result);
    }

    // GET /api/v1/courses/trainer/{trainerId} - Get courses by trainer
    @GetMapping("/trainer/{trainerId}")
    public ResponseEntity<List<CourseSummaryDTO>> getCoursesByTrainer(
            @PathVariable Long trainerId) {
        log.info("REST request to get courses by trainer: {}", trainerId);
        List<CourseSummaryDTO> result = courseService.getCoursesByTrainer(trainerId);
        return ResponseEntity.ok(result);
    }

    // GET /api/v1/courses/search?keyword=something - Search courses
    @GetMapping("/search")
    public ResponseEntity<List<CourseSummaryDTO>> searchCourses(
            @RequestParam String keyword) {
        log.info("REST request to search courses with keyword: {}", keyword);
        List<CourseSummaryDTO> result = courseService.searchCourses(keyword);
        return ResponseEntity.ok(result);
    }

    // PUT /api/v1/courses/{id} - Update course
    @PutMapping("/{id}")
    public ResponseEntity<CourseDTO> updateCourse(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCourseRequestDTO courseDTO) {
        log.info("REST request to update course with ID: {}", id);
        CourseDTO result = courseService.updateCourse(id, courseDTO);
        return ResponseEntity.ok(result);
    }

    // PATCH /api/v1/courses/{id}/status - Update course status
    @PatchMapping("/{id}/status")
    public ResponseEntity<CourseDTO> updateCourseStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        log.info("REST request to update status of course {} to {}", id, status);
        CourseDTO result = courseService.updateCourseStatus(id, status);
        return ResponseEntity.ok(result);
    }

    // PATCH /api/v1/courses/{id}/rating - Update course rating
    @PatchMapping("/{id}/rating")
    public ResponseEntity<CourseDTO> updateCourseRating(
            @PathVariable Long id,
            @RequestParam Double rating) {
        log.info("REST request to update rating of course {} to {}", id, rating);
        CourseDTO result = courseService.updateCourseRating(id, rating);
        return ResponseEntity.ok(result);
    }

    // DELETE /api/v1/courses/{id} - Delete course
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCourse(@PathVariable Long id) {
        log.info("REST request to delete course with ID: {}", id);
        courseService.deleteCourse(id);
        return ResponseEntity.noContent().build();
    }
}