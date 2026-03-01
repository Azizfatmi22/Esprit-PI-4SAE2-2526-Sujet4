package com.example.mscourse.services.impl;

import com.example.mscourse.dto.*;
import com.example.mscourse.entities.*;
import com.example.mscourse.exceptions.ResourceNotFoundException;
import com.example.mscourse.exceptions.UnauthorizedException;
import com.example.mscourse.exceptions.ValidationException;
import com.example.mscourse.repositories.CourseRepository;
import com.example.mscourse.services.interfaces.ICourseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CourseServiceImpl implements ICourseService {

    private final CourseRepository courseRepository;

    @Override
    public CourseDTO createCourse(CreateCourseRequestDTO courseDTO) {
        log.info("Creating new course: {}", courseDTO.getTitle());

        // Validate unique title for trainer
        if (existsByTitleAndTrainer(courseDTO.getTitle(), courseDTO.getTrainerId())) {
            throw new ValidationException("Course with this title already exists for this trainer");
        }

        Course course = new Course();
        course.setTitle(courseDTO.getTitle());
        course.setDescription(courseDTO.getDescription());
        course.setLevel(courseDTO.getLevel());
        course.setPrice(courseDTO.getPrice());
        course.setDurationMinutes(courseDTO.getDurationMinutes());
        course.setTrainerId(courseDTO.getTrainerId());
        course.setThumbnailUrl(courseDTO.getThumbnailUrl());
        course.setStatus(courseDTO.getStatus() != null ? courseDTO.getStatus() : "DRAFT");
        course.setEnrolledStudents(0);
        course.setRating(0.0);

        Course savedCourse = courseRepository.save(course);
        log.info("Course created successfully with ID: {}", savedCourse.getId());

        return mapToDTO(savedCourse);
    }

    @Override
    public CourseDTO getCourseById(Long id) {
        log.info("Fetching course by ID: {}", id);
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + id));
        return mapToDTO(course);
    }

    @Override
    public CourseDTO getCourseWithChapters(Long id) {
        log.info("Fetching course with chapters by ID: {}", id);
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + id));

        CourseDTO courseDTO = mapToDTO(course);

        // Fetch and map chapters with content blocks
        if (course.getChapters() != null && !course.getChapters().isEmpty()) {
            List<ChapterDTO> chapterDTOs = course.getChapters().stream()
                    .sorted((c1, c2) -> Integer.compare(c1.getOrderIndex(), c2.getOrderIndex()))
                    .map(this::mapChapterToDTO)
                    .collect(Collectors.toList());
            courseDTO.setChapters(chapterDTOs);
        }

        return courseDTO;
    }


    @Override
    public Page<CourseSummaryDTO> getAllCourses(Pageable pageable) {
        log.info("Fetching all courses with pagination");
        return courseRepository.findAll(pageable)
                .map(this::mapToSummaryDTO);
    }

    @Override
    public List<CourseSummaryDTO> getCoursesByLevel(Level level) {
        log.info("Fetching courses by level: {}", level);
        return courseRepository.findByLevel(level).stream()
                .map(this::mapToSummaryDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<CourseSummaryDTO> getCoursesByTrainer(Long trainerId) {
        log.info("Fetching courses by trainer: {}", trainerId);
        return courseRepository.findByTrainerId(trainerId).stream()
                .map(this::mapToSummaryDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<CourseSummaryDTO> searchCourses(String keyword) {
        log.info("Searching courses with keyword: {}", keyword);
        return courseRepository.searchCourses(keyword).stream()
                .map(this::mapToSummaryDTO)
                .collect(Collectors.toList());
    }

    @Override
    public CourseDTO updateCourse(Long id, UpdateCourseRequestDTO courseDTO) {
        log.info("Updating course with ID: {}", id);

        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + id));

        // Update fields if provided
        if (courseDTO.getTitle() != null) {
            // Check if new title is unique for this trainer
            if (!course.getTitle().equals(courseDTO.getTitle()) &&
                    existsByTitleAndTrainer(courseDTO.getTitle(), course.getTrainerId())) {
                throw new ValidationException("Course with this title already exists for this trainer");
            }
            course.setTitle(courseDTO.getTitle());
        }

        if (courseDTO.getDescription() != null) {
            course.setDescription(courseDTO.getDescription());
        }

        if (courseDTO.getLevel() != null) {
            course.setLevel(courseDTO.getLevel());
        }

        if (courseDTO.getPrice() != null) {
            course.setPrice(courseDTO.getPrice());
        }

        if (courseDTO.getDurationMinutes() != null) {
            course.setDurationMinutes(courseDTO.getDurationMinutes());
        }

        if (courseDTO.getStatus() != null) {
            course.setStatus(courseDTO.getStatus());
        }

        if (courseDTO.getThumbnailUrl() != null) {
            course.setThumbnailUrl(courseDTO.getThumbnailUrl());
        }

        Course updatedCourse = courseRepository.save(course);
        log.info("Course updated successfully");

        return mapToDTO(updatedCourse);
    }

    @Override
    public CourseDTO updateCourseStatus(Long id, String status) {
        log.info("Updating status of course {} to: {}", id, status);

        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + id));

        course.setStatus(status);
        Course updatedCourse = courseRepository.save(course);

        return mapToDTO(updatedCourse);
    }

    @Override
    public CourseDTO updateCourseRating(Long id, Double rating) {
        log.info("Updating rating of course {} to: {}", id, rating);

        if (rating < 0 || rating > 5) {
            throw new ValidationException("Rating must be between 0 and 5");
        }

        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + id));

        course.setRating(rating);
        Course updatedCourse = courseRepository.save(course);

        return mapToDTO(updatedCourse);
    }

    @Override
    public CourseDTO updateCourseThumbnail(Long id, String thumbnailUrl) {
        log.info("Updating thumbnail of course {}", id);

        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + id));

        course.setThumbnailUrl(thumbnailUrl);
        Course updatedCourse = courseRepository.save(course);

        return mapToDTO(updatedCourse);
    }

    @Override
    public void deleteCourse(Long id) {
        log.info("Deleting course with ID: {}", id);

        if (!courseRepository.existsById(id)) {
            throw new ResourceNotFoundException("Course not found with id: " + id);
        }

        courseRepository.deleteById(id);
        log.info("Course deleted successfully");
    }

    @Override
    public CourseStatisticsDTO getCourseStatistics(Long trainerId) {
        log.info("Getting course statistics for trainer: {}", trainerId);

        List<Course> courses = courseRepository.findByTrainerId(trainerId);

        Map<Level, Long> coursesByLevel = courses.stream()
                .collect(Collectors.groupingBy(Course::getLevel, Collectors.counting()));

        Map<String, Long> coursesByStatus = courses.stream()
                .collect(Collectors.groupingBy(Course::getStatus, Collectors.counting()));

        long totalEnrollments = courses.stream()
                .mapToInt(c -> c.getEnrolledStudents() != null ? c.getEnrolledStudents() : 0)
                .sum();

        double averageRating = courses.stream()
                .filter(c -> c.getRating() != null)
                .mapToDouble(Course::getRating)
                .average()
                .orElse(0.0);

        long totalChapters = courses.stream()
                .mapToLong(c -> c.getChapters() != null ? c.getChapters().size() : 0)
                .sum();

        long totalContentBlocks = courses.stream()
                .flatMap(c -> c.getChapters().stream())
                .mapToLong(ch -> ch.getContentBlocks() != null ? ch.getContentBlocks().size() : 0)
                .sum();

        return CourseStatisticsDTO.builder()
                .trainerId(trainerId)
                .totalCourses((long) courses.size())
                .publishedCourses(coursesByStatus.getOrDefault("PUBLISHED", 0L))
                .draftCourses(coursesByStatus.getOrDefault("DRAFT", 0L))
                .archivedCourses(coursesByStatus.getOrDefault("ARCHIVED", 0L))
                .totalEnrollments(totalEnrollments)
                .averageRating(averageRating)
                .coursesByLevel(coursesByLevel)
                .coursesByStatus(coursesByStatus)
                .totalChapters(totalChapters)
                .totalContentBlocks(totalContentBlocks)
                .build();
    }

    @Override
    public List<CourseSummaryDTO> getTopRatedCourses(int limit) {
        log.info("Fetching top {} rated courses", limit);
        return courseRepository.findTop10ByOrderByCreatedAtDesc().stream()
                .filter(c -> c.getRating() != null)
                .sorted((c1, c2) -> Double.compare(c2.getRating(), c1.getRating()))
                .limit(limit)
                .map(this::mapToSummaryDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<CourseSummaryDTO> getMostEnrolledCourses(int limit) {
        log.info("Fetching top {} most enrolled courses", limit);
        return courseRepository.findAll().stream()
                .sorted((c1, c2) -> Integer.compare(
                        c2.getEnrolledStudents() != null ? c2.getEnrolledStudents() : 0,
                        c1.getEnrolledStudents() != null ? c1.getEnrolledStudents() : 0))
                .limit(limit)
                .map(this::mapToSummaryDTO)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsByTitleAndTrainer(String title, Long trainerId) {
        return courseRepository.existsByTitleAndTrainerId(title, trainerId);
    }

    // Mapping methods
    private CourseDTO mapToDTO(Course course) {
        CourseDTO.CourseDTOBuilder builder = CourseDTO.builder()
                .id(course.getId())
                .title(course.getTitle())
                .description(course.getDescription())
                .level(course.getLevel())
                .price(course.getPrice())
                .durationMinutes(course.getDurationMinutes())
                .status(course.getStatus())
                .trainerId(course.getTrainerId())
                .enrolledStudents(course.getEnrolledStudents())
                .rating(course.getRating())
                .thumbnailUrl(course.getThumbnailUrl())
                .createdAt(course.getCreatedAt())
                .updatedAt(course.getUpdatedAt());

        if (course.getChapters() != null) {
            builder.totalChapters(course.getChapters().size());
            builder.chapters(course.getChapters().stream()
                    .map(this::mapChapterToDTO)
                    .collect(Collectors.toList()));
        }

        if (course.getAttachments() != null) {
            builder.attachments(course.getAttachments().stream()
                    .map(this::mapAttachmentToDTO)
                    .collect(Collectors.toList()));
        }

        return builder.build();
    }

    private CourseSummaryDTO mapToSummaryDTO(Course course) {
        int totalChapters = course.getChapters() != null ? course.getChapters().size() : 0;

        return CourseSummaryDTO.builder()
                .id(course.getId())
                .title(course.getTitle())
                .description(course.getDescription())
                .level(course.getLevel())
                .price(course.getPrice())
                .status(course.getStatus())
                .trainerId(course.getTrainerId())
                .enrolledStudents(course.getEnrolledStudents())
                .rating(course.getRating())
                .thumbnailUrl(course.getThumbnailUrl())
                .totalChapters(totalChapters)
                .totalDurationMinutes(course.getDurationMinutes())
                .build();
    }

    private ChapterDTO mapChapterToDTO(Chapter chapter) {
        ChapterDTO chapterDTO = ChapterDTO.builder()
                .id(chapter.getId())
                .title(chapter.getTitle())
                .description(chapter.getDescription())
                .orderIndex(chapter.getOrderIndex())
                .courseId(chapter.getCourse().getId())
                .totalContentBlocks(chapter.getContentBlocks() != null ? chapter.getContentBlocks().size() : 0)
                .build();
        
        // Include content blocks if they exist
        if (chapter.getContentBlocks() != null && !chapter.getContentBlocks().isEmpty()) {
            List<ContentBlockDTO> contentBlockDTOs = chapter.getContentBlocks().stream()
                    .sorted((cb1, cb2) -> Integer.compare(cb1.getOrderIndex(), cb2.getOrderIndex()))
                    .map(this::mapContentBlockToDTO)
                    .collect(Collectors.toList());
            chapterDTO.setContentBlocks(contentBlockDTOs);
        }
        
        return chapterDTO;
    }
    
    private ContentBlockDTO mapContentBlockToDTO(ContentBlock contentBlock) {
        return ContentBlockDTO.builder()
                .id(contentBlock.getId())
                .type(contentBlock.getType())
                .title(contentBlock.getTitle())
                .data(contentBlock.getData())
                .orderIndex(contentBlock.getOrderIndex())
                .chapterId(contentBlock.getChapter().getId())
                .build();
    }

    private CourseAttachmentDTO mapAttachmentToDTO(CourseAttachment attachment) {
        return CourseAttachmentDTO.builder()
                .id(attachment.getId())
                .fileName(attachment.getFileName())
                .fileType(attachment.getFileType())
                .fileSize(attachment.getFileSize())
                .fileUrl(attachment.getFileUrl())
                .category(attachment.getCategory())
                .description(attachment.getDescription())
                .courseId(attachment.getCourse().getId())
                .build();
    }
}