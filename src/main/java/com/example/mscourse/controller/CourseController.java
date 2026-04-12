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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
@Slf4j
public class CourseController {

    private final ICourseService courseService;
    private final com.example.mscourse.services.interfaces.IChapterService chapterService;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    // ==================== CREATE OPERATIONS ====================

    @Value("${file.upload.dir}")
    private String fileUploadDir;

    @GetMapping("/{id}/title")
    public ResponseEntity<String> getCourseTitle(@PathVariable Long id) {
        log.info("REST request to get title for course ID: {}", id);
        try {
            CourseDTO course = courseService.getCourseById(id);
            return ResponseEntity.ok(course.getTitle());
        } catch (Exception e) {
            log.error("Course not found or error occurred: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("formini_course_not_found");
        }
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CourseDTO> createCourse(
            @RequestParam String title,
            @RequestParam String description,
            @RequestParam Level level,
            @RequestParam Double price,
            @RequestParam(required = false) Integer durationMinutes,
            @RequestParam(required = false, defaultValue = "DRAFT") String status,
            @RequestParam String trainerId,
            @RequestParam(required = false) MultipartFile thumbnail,
            @RequestParam(required = false) String chaptersJson) {

        log.info("REST request to create course: {} with status: {}", title, status);
        log.info("File upload directory: {}", fileUploadDir);

        try {
            // Create course first WITHOUT thumbnail (to get the courseId)
            CreateCourseRequestDTO courseDTO = CreateCourseRequestDTO.builder()
                    .title(title)
                    .description(description)
                    .level(level)
                    .price(price)
                    .durationMinutes(durationMinutes)
                    .status(status)
                    .trainerId(trainerId)
                    .thumbnailUrl(null) // Will be set after course creation
                    .build();

            CourseDTO createdCourse = courseService.createCourse(courseDTO);
            Long courseId = createdCourse.getId();
            log.info("Course created with ID: {}", courseId);

            if (chaptersJson != null && !chaptersJson.trim().isEmpty()) {
                try {
                    com.fasterxml.jackson.core.type.TypeReference<List<CreateChapterRequestDTO>> typeRef =
                        new com.fasterxml.jackson.core.type.TypeReference<List<CreateChapterRequestDTO>>() {};
                    List<CreateChapterRequestDTO> chapters = objectMapper.readValue(chaptersJson, typeRef);
                    for (CreateChapterRequestDTO chapter : chapters) {
                        chapterService.createChapter(courseId, chapter);
                    }
                    log.info("Created {} chapters for course ID: {}", chapters.size(), courseId);
                } catch (Exception e) {
                    log.error("Error parsing or creating chapters from JSON: {}", e.getMessage(), e);
                }
            }

            // Ensure standard folder structure exists for each course
            ensureCourseDirectoryStructureSafely(courseId);

            // Process thumbnail AFTER course creation (so we have the courseId)
            String thumbnailFilename = null;
            if (thumbnail != null && !thumbnail.isEmpty()) {
                // Generate unique filename
                String originalFilename = thumbnail.getOriginalFilename();
                String extension = "";
                if (originalFilename != null && originalFilename.contains(".")) {
                    extension = originalFilename.substring(originalFilename.lastIndexOf("."));
                } else {
                    extension = ".jpg"; // default extension
                }

                thumbnailFilename = System.currentTimeMillis() + "_" +
                        (originalFilename != null ? originalFilename.replaceAll("[^a-zA-Z0-9.-]", "_") : "image") + extension;

                // Create course-specific thumbnail directory: uploads/cours_{courseId}/thumbnails/
                String thumbnailDir = fileUploadDir + "/cours_" + courseId + "/thumbnails/";
                Path dirPath = Paths.get(thumbnailDir);
                if (!Files.exists(dirPath)) {
                    Files.createDirectories(dirPath);
                    log.info("Created directory: {}", thumbnailDir);
                }

                // Save the file
                Path filePath = Paths.get(thumbnailDir + thumbnailFilename);
                Files.write(filePath, thumbnail.getBytes());

                log.info("Thumbnail saved: {} ({} bytes)", thumbnailFilename, thumbnail.getSize());

                // Update course with thumbnail URL
                createdCourse = courseService.updateCourseThumbnail(courseId, thumbnailFilename);
            }

            // Fetch the course again so the returned object includes all the newly attached chapters
            CourseDTO finalCourseData = courseService.getCourseById(courseId);

            return new ResponseEntity<>(finalCourseData, HttpStatus.CREATED);

        } catch (IOException e) {
            log.error("Error processing file upload: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    // ==================== READ OPERATIONS ====================

    @GetMapping("/{id}")
    public ResponseEntity<CourseDTO> getCourseById(@PathVariable Long id) {
        log.info("REST request to get course by ID: {}", id);
        CourseDTO result = courseService.getCourseById(id);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}/with-chapters")
    public ResponseEntity<CourseDTO> getCourseWithChapters(@PathVariable Long id) {
        log.info("REST request to get course with chapters by ID: {}", id);
        CourseDTO result = courseService.getCourseWithChapters(id);
        return ResponseEntity.ok(result);
    }

    @GetMapping
    public ResponseEntity<Page<CourseSummaryDTO>> getAllCourses(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        log.info("REST request to get all courses");
        Page<CourseSummaryDTO> result = courseService.getAllCourses(pageable);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/level/{level}")
    public ResponseEntity<List<CourseSummaryDTO>> getCoursesByLevel(@PathVariable Level level) {
        log.info("REST request to get courses by level: {}", level);
        List<CourseSummaryDTO> result = courseService.getCoursesByLevel(level);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/trainer/{trainerId}")
    public ResponseEntity<List<CourseSummaryDTO>> getCoursesByTrainer(@PathVariable String trainerId) {
        log.info("REST request to get courses by trainer: {}", trainerId);
        List<CourseSummaryDTO> result = courseService.getCoursesByTrainer(trainerId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/search")
    public ResponseEntity<List<CourseSummaryDTO>> searchCourses(@RequestParam String keyword) {
        log.info("REST request to search courses with keyword: {}", keyword);
        List<CourseSummaryDTO> result = courseService.searchCourses(keyword);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/top-rated")
    public ResponseEntity<List<CourseSummaryDTO>> getTopRatedCourses(
            @RequestParam(defaultValue = "10") int limit) {
        log.info("REST request to get top {} rated courses", limit);
        List<CourseSummaryDTO> result = courseService.getTopRatedCourses(limit);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/most-enrolled")
    public ResponseEntity<List<CourseSummaryDTO>> getMostEnrolledCourses(
            @RequestParam(defaultValue = "10") int limit) {
        log.info("REST request to get top {} most enrolled courses", limit);
        List<CourseSummaryDTO> result = courseService.getMostEnrolledCourses(limit);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/trainer/{trainerId}/statistics")
    public ResponseEntity<CourseStatisticsDTO> getCourseStatistics(@PathVariable String trainerId) {
        log.info("REST request to get course statistics for trainer: {}", trainerId);
        CourseStatisticsDTO result = courseService.getCourseStatistics(trainerId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/exists")
    public ResponseEntity<Boolean> checkCourseExists(
            @RequestParam String title,
            @RequestParam String trainerId) {
        log.info("REST request to check if course exists with title: {} for trainer: {}", title, trainerId);
        boolean exists = courseService.existsByTitleAndTrainer(title, trainerId);
        return ResponseEntity.ok(exists);
    }

    // ==================== UPDATE OPERATIONS ====================

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CourseDTO> updateCourseWithFile(
            @PathVariable Long id,
            @RequestParam String title,
            @RequestParam String description,
            @RequestParam Level level,
            @RequestParam Double price,
            @RequestParam(required = false) Integer durationMinutes,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) MultipartFile thumbnail) {

        log.info("REST request to update course with ID: {} (multipart)", id);

        try {
            ensureCourseDirectoryStructureSafely(id);

            // Process thumbnail if provided
            String thumbnailFilename = null;
            if (thumbnail != null && !thumbnail.isEmpty()) {
                String originalFilename = thumbnail.getOriginalFilename();
                String extension = "";
                if (originalFilename != null && originalFilename.contains(".")) {
                    extension = originalFilename.substring(originalFilename.lastIndexOf("."));
                } else {
                    extension = ".jpg";
                }

                thumbnailFilename = System.currentTimeMillis() + "_" +
                        (originalFilename != null ? originalFilename.replaceAll("[^a-zA-Z0-9.-]", "_") : "image") + extension;

                // Save to course-specific directory: uploads/cours_{courseId}/thumbnails/
                String thumbnailDir = fileUploadDir + "/cours_" + id + "/thumbnails/";
                Path dirPath = Paths.get(thumbnailDir);
                if (!Files.exists(dirPath)) {
                    Files.createDirectories(dirPath);
                }

                Path filePath = Paths.get(thumbnailDir + thumbnailFilename);
                Files.write(filePath, thumbnail.getBytes());
                log.info("Thumbnail updated: {}", thumbnailFilename);
            }

            UpdateCourseRequestDTO courseDTO = UpdateCourseRequestDTO.builder()
                    .title(title)
                    .description(description)
                    .level(level)
                    .price(price)
                    .durationMinutes(durationMinutes)
                    .status(status)
                    .thumbnailUrl(thumbnailFilename)
                    .build();

            CourseDTO result = courseService.updateCourse(id, courseDTO);
            return ResponseEntity.ok(result);

        } catch (IOException e) {
            log.error("Error processing file upload during update: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<CourseDTO> updateCourse(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCourseRequestDTO courseDTO) {
        log.info("REST request to update course with ID: {} (JSON)", id);
        ensureCourseDirectoryStructureSafely(id);
        CourseDTO result = courseService.updateCourse(id, courseDTO);
        return ResponseEntity.ok(result);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<CourseDTO> updateCourseStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        log.info("REST request to update status of course {} to: {}", id, status);
        CourseDTO result = courseService.updateCourseStatus(id, status);
        return ResponseEntity.ok(result);
    }

    @PatchMapping("/{id}/rating")
    public ResponseEntity<CourseDTO> updateCourseRating(
            @PathVariable Long id,
            @RequestParam Double rating) {
        log.info("REST request to update rating of course {} to: {}", id, rating);
        CourseDTO result = courseService.updateCourseRating(id, rating);
        return ResponseEntity.ok(result);
    }

    @PatchMapping("/{id}/thumbnail")
    public ResponseEntity<CourseDTO> updateCourseThumbnail(
            @PathVariable Long id,
            @RequestParam MultipartFile thumbnail) throws IOException {
        log.info("REST request to update thumbnail of course: {}", id);

        try {
            ensureCourseDirectoryStructureSafely(id);

            // Generate unique filename
            String originalFilename = thumbnail.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            } else {
                extension = ".jpg";
            }

            String thumbnailFilename = System.currentTimeMillis() + "_" +
                    (originalFilename != null ? originalFilename.replaceAll("[^a-zA-Z0-9.-]", "_") : "image") + extension;

            // Save to course-specific directory: uploads/cours_{courseId}/thumbnails/
            String thumbnailDir = fileUploadDir + "/cours_" + id + "/thumbnails/";
            Path dirPath = Paths.get(thumbnailDir);
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
            }

            Path filePath = Paths.get(thumbnailDir + thumbnailFilename);
            Files.write(filePath, thumbnail.getBytes());
            log.info("Thumbnail updated: {}", thumbnailFilename);

            CourseDTO result = courseService.updateCourseThumbnail(id, thumbnailFilename);
            return ResponseEntity.ok(result);
        } catch (IOException e) {
            log.error("Error uploading thumbnail: {}", e.getMessage());
            throw e;
        }
    }

    // ==================== DELETE OPERATIONS ====================

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCourse(@PathVariable Long id) {
        log.info("REST request to delete course with ID: {}", id);
        courseService.deleteCourse(id);
        return ResponseEntity.noContent().build();
    }

    private void ensureCourseDirectoryStructureSafely(Long courseId) {
        try {
            Path courseRoot = Paths.get(fileUploadDir, "cours_" + courseId);
            Path thumbnailsDir = courseRoot.resolve("thumbnails");
            Path chaptersDir = courseRoot.resolve("chapters");
            Path attachmentsDir = courseRoot.resolve("attachments");

            if (!Files.exists(courseRoot)) {
                Files.createDirectories(courseRoot);
                log.info("Created course root directory: {}", courseRoot);
            }
            if (!Files.exists(thumbnailsDir)) {
                Files.createDirectories(thumbnailsDir);
                log.info("Created thumbnails directory: {}", thumbnailsDir);
            }
            if (!Files.exists(chaptersDir)) {
                Files.createDirectories(chaptersDir);
                log.info("Created chapters directory: {}", chaptersDir);
            }
            if (!Files.exists(attachmentsDir)) {
                Files.createDirectories(attachmentsDir);
                log.info("Created attachments directory: {}", attachmentsDir);
            }
        } catch (IOException e) {
            log.warn("Failed to ensure course folder structure for course {}", courseId, e);
        }
    }
}