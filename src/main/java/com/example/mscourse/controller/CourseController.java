package com.example.mscourse.controller;

import com.example.mscourse.dto.*;
import com.example.mscourse.entities.Level;
import com.example.mscourse.services.interfaces.ICourseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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

    // ── Constants ────────────────────────────────────────────────────────────
    private static final String DEFAULT_IMAGE_NAME   = "image";
    private static final String DEFAULT_EXTENSION    = ".jpg";
    private static final String FILENAME_SAFE_REGEX  = "[^a-zA-Z0-9.-]";
    private static final String COURSE_DIR_PREFIX    = "/cours_";
    private static final String THUMBNAILS_SUBDIR    = "/thumbnails/";

    // ── Dependencies ─────────────────────────────────────────────────────────
    private final ICourseService courseService;
    private final com.example.mscourse.services.interfaces.IChapterService chapterService;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @Value("${file.upload.dir}")
    private String fileUploadDir;

    // ==================== CREATE OPERATIONS ====================

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

        try {
            CourseDTO createdCourse = courseService.createCourse(
                    buildCreateCourseRequest(title, description, level, price, durationMinutes, status, trainerId));
            Long courseId = createdCourse.getId();
            log.info("Course created with ID: {}", courseId);

            ensureCourseDirectoryStructureSafely(courseId);
            processChaptersJson(courseId, chaptersJson);
            processThumbnail(courseId, thumbnail);

            return new ResponseEntity<>(courseService.getCourseById(courseId), HttpStatus.CREATED);

        } catch (IOException e) {
            log.error("Error processing file upload: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ==================== READ OPERATIONS ====================

    @GetMapping("/{id}")
    public ResponseEntity<CourseDTO> getCourseById(@PathVariable Long id) {
        log.info("REST request to get course by ID: {}", id);
        return ResponseEntity.ok(courseService.getCourseById(id));
    }

    @GetMapping("/{id}/with-chapters")
    public ResponseEntity<CourseDTO> getCourseWithChapters(@PathVariable Long id) {
        log.info("REST request to get course with chapters by ID: {}", id);
        return ResponseEntity.ok(courseService.getCourseWithChapters(id));
    }

    @GetMapping
    public ResponseEntity<Page<CourseSummaryDTO>> getAllCourses(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        log.info("REST request to get all courses");
        return ResponseEntity.ok(courseService.getAllCourses(pageable));
    }

    @GetMapping("/level/{level}")
    public ResponseEntity<List<CourseSummaryDTO>> getCoursesByLevel(@PathVariable Level level) {
        log.info("REST request to get courses by level: {}", level);
        return ResponseEntity.ok(courseService.getCoursesByLevel(level));
    }

    @GetMapping("/trainer/{trainerId}")
    public ResponseEntity<List<CourseSummaryDTO>> getCoursesByTrainer(@PathVariable String trainerId) {
        log.info("REST request to get courses by trainer: {}", trainerId);
        return ResponseEntity.ok(courseService.getCoursesByTrainer(trainerId));
    }

    @GetMapping("/search")
    public ResponseEntity<List<CourseSummaryDTO>> searchCourses(@RequestParam String keyword) {
        log.info("REST request to search courses with keyword: {}", keyword);
        return ResponseEntity.ok(courseService.searchCourses(keyword));
    }

    @GetMapping("/top-rated")
    public ResponseEntity<List<CourseSummaryDTO>> getTopRatedCourses(
            @RequestParam(defaultValue = "10") int limit) {
        log.info("REST request to get top {} rated courses", limit);
        return ResponseEntity.ok(courseService.getTopRatedCourses(limit));
    }

    @GetMapping("/most-enrolled")
    public ResponseEntity<List<CourseSummaryDTO>> getMostEnrolledCourses(
            @RequestParam(defaultValue = "10") int limit) {
        log.info("REST request to get top {} most enrolled courses", limit);
        return ResponseEntity.ok(courseService.getMostEnrolledCourses(limit));
    }

    @GetMapping("/trainer/{trainerId}/statistics")
    public ResponseEntity<CourseStatisticsDTO> getCourseStatistics(@PathVariable String trainerId) {
        log.info("REST request to get course statistics for trainer: {}", trainerId);
        return ResponseEntity.ok(courseService.getCourseStatistics(trainerId));
    }

    @GetMapping("/exists")
    public ResponseEntity<Boolean> checkCourseExists(
            @RequestParam String title,
            @RequestParam String trainerId) {
        log.info("REST request to check if course exists: {} / {}", title, trainerId);
        return ResponseEntity.ok(courseService.existsByTitleAndTrainer(title, trainerId));
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
            String thumbnailFilename = saveThumbnailFile(id, thumbnail);

            UpdateCourseRequestDTO courseDTO = UpdateCourseRequestDTO.builder()
                    .title(title)
                    .description(description)
                    .level(level)
                    .price(price)
                    .durationMinutes(durationMinutes)
                    .status(status)
                    .thumbnailUrl(thumbnailFilename)
                    .build();

            return ResponseEntity.ok(courseService.updateCourse(id, courseDTO));

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
        return ResponseEntity.ok(courseService.updateCourse(id, courseDTO));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<CourseDTO> updateCourseStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        log.info("REST request to update status of course {} to: {}", id, status);
        return ResponseEntity.ok(courseService.updateCourseStatus(id, status));
    }

    @PatchMapping("/{id}/rating")
    public ResponseEntity<CourseDTO> updateCourseRating(
            @PathVariable Long id,
            @RequestParam Double rating) {
        log.info("REST request to update rating of course {} to: {}", id, rating);
        return ResponseEntity.ok(courseService.updateCourseRating(id, rating));
    }

    @PatchMapping("/{id}/thumbnail")
    public ResponseEntity<CourseDTO> updateCourseThumbnail(
            @PathVariable Long id,
            @RequestParam MultipartFile thumbnail) throws IOException {
        log.info("REST request to update thumbnail of course: {}", id);
        try {
            ensureCourseDirectoryStructureSafely(id);
            String thumbnailFilename = saveThumbnailFile(id, thumbnail);
            return ResponseEntity.ok(courseService.updateCourseThumbnail(id, thumbnailFilename));
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

    // ==================== PRIVATE HELPERS ====================

    /**
     * Builds a CreateCourseRequestDTO from individual request params.
     */
    private CreateCourseRequestDTO buildCreateCourseRequest(
            String title, String description, Level level, Double price,
            Integer durationMinutes, String status, String trainerId) {
        return CreateCourseRequestDTO.builder()
                .title(title)
                .description(description)
                .level(level)
                .price(price)
                .durationMinutes(durationMinutes)
                .status(status)
                .trainerId(trainerId)
                .thumbnailUrl(null)
                .build();
    }

    /**
     * Parses chaptersJson and creates chapters for the given course.
     * Errors are logged but do not abort the request.
     */
    private void processChaptersJson(Long courseId, String chaptersJson) {
        if (chaptersJson == null || chaptersJson.trim().isEmpty()) {
            return;
        }
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

    /**
     * Saves the thumbnail file and updates the course record.
     * Returns the saved filename, or null if no thumbnail was provided.
     */
    private void processThumbnail(Long courseId, MultipartFile thumbnail) throws IOException {
        if (thumbnail == null || thumbnail.isEmpty()) {
            return;
        }
        String filename = saveThumbnailFile(courseId, thumbnail);
        courseService.updateCourseThumbnail(courseId, filename);
    }

    /**
     * Saves a thumbnail MultipartFile to the course-specific thumbnails directory.
     * Returns the generated filename, or null if the file is empty/null.
     */
    private String saveThumbnailFile(Long courseId, MultipartFile thumbnail) throws IOException {
        if (thumbnail == null || thumbnail.isEmpty()) {
            return null;
        }

        String originalFilename = thumbnail.getOriginalFilename();
        String extension = resolveExtension(originalFilename);
        String safeOriginal = originalFilename != null
                ? originalFilename.replaceAll(FILENAME_SAFE_REGEX, "_")
                : DEFAULT_IMAGE_NAME;
        String filename = System.currentTimeMillis() + "_" + safeOriginal + extension;

        String thumbnailDir = fileUploadDir + COURSE_DIR_PREFIX + courseId + THUMBNAILS_SUBDIR;
        Path dirPath = Paths.get(thumbnailDir);
        if (!Files.exists(dirPath)) {
            Files.createDirectories(dirPath);
            log.info("Created directory: {}", thumbnailDir);
        }

        Files.write(Paths.get(thumbnailDir + filename), thumbnail.getBytes());
        log.info("Thumbnail saved: {} ({} bytes)", filename, thumbnail.getSize());
        return filename;
    }

    /**
     * Resolves the file extension from the original filename.
     * Falls back to ".jpg" if no extension is found.
     */
    private String resolveExtension(String originalFilename) {
        if (originalFilename != null && originalFilename.contains(".")) {
            return originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        return DEFAULT_EXTENSION;
    }

    /**
     * Creates the standard directory structure for a course (thumbnails, chapters, attachments).
     * Errors are logged as warnings and do not propagate.
     */
    private void ensureCourseDirectoryStructureSafely(Long courseId) {
        try {
            Path courseRoot     = Paths.get(fileUploadDir, "cours_" + courseId);
            Path thumbnailsDir  = courseRoot.resolve("thumbnails");
            Path chaptersDir    = courseRoot.resolve("chapters");
            Path attachmentsDir = courseRoot.resolve("attachments");

            createIfAbsent(courseRoot);
            createIfAbsent(thumbnailsDir);
            createIfAbsent(chaptersDir);
            createIfAbsent(attachmentsDir);
        } catch (IOException e) {
            log.warn("Failed to ensure course folder structure for course {}", courseId, e);
        }
    }

    private void createIfAbsent(Path path) throws IOException {
        if (!Files.exists(path)) {
            Files.createDirectories(path);
            log.info("Created directory: {}", path);
        }
    }
}
