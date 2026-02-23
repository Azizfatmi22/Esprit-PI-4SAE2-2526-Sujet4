package com.example.mscourse.services.impl;

import com.example.mscourse.dto.*;
import com.example.mscourse.entities.*;
import com.example.mscourse.Exceptions.CourseNotFoundException;
import com.example.mscourse.repositories.ChapterRepository;
import com.example.mscourse.repositories.ContentBlockRepository;
import com.example.mscourse.repositories.CourseRepository;
import com.example.mscourse.services.interfaces.ICourseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CourseServiceImpl implements ICourseService {

    private final CourseRepository courseRepository;
    private final ChapterRepository chapterRepository;
    private final ContentBlockRepository contentBlockRepository;

    @Override
    public CourseDTO createCourse(CreateCourseRequestDTO requestDTO) {
        log.info("Creating new course: {}", requestDTO.getTitle());

        Course course = new Course();
        course.setTitle(requestDTO.getTitle());
        course.setDescription(requestDTO.getDescription());
        course.setLevel(requestDTO.getLevel());
        course.setPrice(requestDTO.getPrice());
        course.setDuration(requestDTO.getDuration());
        course.setTrainerId(requestDTO.getTrainerId() != null ? requestDTO.getTrainerId() : 1L);
        course.setThumbnail(requestDTO.getThumbnail());
        course.setStatus("DRAFT");
        course.setEnrolledStudents(0);
        course.setRating(0.0);

        // Save course first
        Course savedCourse = courseRepository.save(course);
        log.info("Course created with ID: {}", savedCourse.getId());

        // Handle chapters if provided
        if (requestDTO.getChapters() != null && !requestDTO.getChapters().isEmpty()) {
            List<Chapter> chapters = requestDTO.getChapters().stream()
                    .map(chapterDTO -> createChapterFromDTO(chapterDTO, savedCourse))
                    .collect(Collectors.toList());
            savedCourse.setChapters(chapters);
            log.info("Created {} chapters for course {}", chapters.size(), savedCourse.getId());
        }

        return convertToDTO(savedCourse);
    }

    @Override
    public CourseDTO getCourseById(Long id) {
        log.info("Fetching course with ID: {}", id);
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new CourseNotFoundException("Course not found with ID: " + id));
        return convertToDTO(course);
    }

    @Override
    public Page<CourseSummaryDTO> getAllCourses(Pageable pageable) {
        log.info("Fetching all courses with pagination");
        return courseRepository.findAll(pageable)
                .map(this::convertToSummaryDTO);
    }

    @Override
    public List<CourseSummaryDTO> getCoursesByLevel(Level level) {
        log.info("Fetching courses by level: {}", level);
        return courseRepository.findByLevel(level)
                .stream()
                .map(this::convertToSummaryDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<CourseSummaryDTO> getCoursesByTrainer(Long trainerId) {
        log.info("Fetching courses by trainer: {}", trainerId);
        return courseRepository.findByTrainerId(trainerId)
                .stream()
                .map(this::convertToSummaryDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<CourseSummaryDTO> searchCourses(String keyword) {
        log.info("Searching courses with keyword: {}", keyword);
        return courseRepository.searchCourses(keyword)
                .stream()
                .map(this::convertToSummaryDTO)
                .collect(Collectors.toList());
    }

    @Override
    public CourseDTO updateCourse(Long id, UpdateCourseRequestDTO requestDTO) {
        log.info("Updating course with ID: {}", id);
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new CourseNotFoundException("Course not found with ID: " + id));

        if (requestDTO.getTitle() != null) {
            course.setTitle(requestDTO.getTitle());
        }
        if (requestDTO.getDescription() != null) {
            course.setDescription(requestDTO.getDescription());
        }
        if (requestDTO.getLevel() != null) {
            course.setLevel(requestDTO.getLevel());
        }
        if (requestDTO.getPrice() != null) {
            course.setPrice(requestDTO.getPrice());
        }
        if (requestDTO.getStatus() != null) {
            course.setStatus(requestDTO.getStatus());
        }
        if (requestDTO.getThumbnail() != null) {
            course.setThumbnail(requestDTO.getThumbnail());
        }

        Course updatedCourse = courseRepository.save(course);
        return convertToDTO(updatedCourse);
    }

    @Override
    public CourseDTO updateCourseStatus(Long id, String status) {
        log.info("Updating status of course {} to {}", id, status);
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new CourseNotFoundException("Course not found with ID: " + id));
        course.setStatus(status);
        Course updatedCourse = courseRepository.save(course);
        return convertToDTO(updatedCourse);
    }

    @Override
    public CourseDTO updateCourseRating(Long id, Double rating) {
        log.info("Updating rating of course {} to {}", id, rating);
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new CourseNotFoundException("Course not found with ID: " + id));
        course.setRating(rating);
        Course updatedCourse = courseRepository.save(course);
        return convertToDTO(updatedCourse);
    }

    @Override
    public void deleteCourse(Long id) {
        log.info("Deleting course with ID: {}", id);
        if (!courseRepository.existsById(id)) {
            throw new CourseNotFoundException("Course not found with ID: " + id);
        }
        courseRepository.deleteById(id);
        log.info("Course deleted successfully");
    }

    @Override
    public ChapterDTO addChapterToCourse(Long courseId, ChapterDTO chapterDTO) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public List<ChapterDTO> getCourseChapters(Long courseId) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    // Helper method to convert ChapterDTO to Chapter entity
    private Chapter createChapterFromDTO(ChapterDTO chapterDTO, Course course) {
        Chapter chapter = new Chapter();
        chapter.setTitle(chapterDTO.getTitle());
        chapter.setOrderIndex(chapterDTO.getId() != null ? Math.toIntExact(chapterDTO.getId()) : 0);
        chapter.setCourse(course);

        Chapter savedChapter = chapterRepository.save(chapter);

        // Handle content blocks
        if (chapterDTO.getContentBlocks() != null && !chapterDTO.getContentBlocks().isEmpty()) {
            List<ContentBlock> contentBlocks = chapterDTO.getContentBlocks().stream()
                    .map(blockDTO -> createContentBlockFromDTO(blockDTO, savedChapter))
                    .collect(Collectors.toList());
            savedChapter.setContentBlocks(contentBlocks);
        }

        // Handle videos
        if (chapterDTO.getVideos() != null && !chapterDTO.getVideos().isEmpty()) {
            List<ChapterVideo> videos = chapterDTO.getVideos().stream()
                    .map(videoDTO -> {
                        ChapterVideo video = new ChapterVideo();
                        video.setName(videoDTO.getName());
                        video.setUrl(videoDTO.getUrl());
                        video.setSize(videoDTO.getSize());
                        video.setType(videoDTO.getType());
                        video.setDuration(videoDTO.getDuration());
                        video.setChapter(savedChapter);
                        return video;
                    })
                    .collect(Collectors.toList());
            savedChapter.setVideos(videos);
        }

        // Handle images
        if (chapterDTO.getImages() != null && !chapterDTO.getImages().isEmpty()) {
            List<ChapterImage> images = chapterDTO.getImages().stream()
                    .map(imageDTO -> {
                        ChapterImage image = new ChapterImage();
                        image.setName(imageDTO.getName());
                        image.setUrl(imageDTO.getUrl());
                        image.setSize(imageDTO.getSize());
                        image.setType(imageDTO.getType());
                        image.setChapter(savedChapter);
                        return image;
                    })
                    .collect(Collectors.toList());
            savedChapter.setImages(images);
        }

        // Handle files
        if (chapterDTO.getFiles() != null && !chapterDTO.getFiles().isEmpty()) {
            List<ChapterFile> files = chapterDTO.getFiles().stream()
                    .map(fileDTO -> {
                        ChapterFile file = new ChapterFile();
                        file.setFileName(fileDTO.getFileName());
                        file.setFileType(fileDTO.getFileType());
                        file.setFileSize(fileDTO.getFileSize());
                        file.setFileUrl(fileDTO.getFileUrl());
                        file.setDescription(fileDTO.getDescription());
                        file.setChapter(savedChapter);
                        return file;
                    })
                    .collect(Collectors.toList());
            savedChapter.setFiles(files);
        }

        return chapterRepository.save(savedChapter);
    }

    // Helper method to convert ContentBlockDTO to ContentBlock entity
    private ContentBlock createContentBlockFromDTO(ContentBlockDTO blockDTO, Chapter chapter) {
        ContentBlock contentBlock = new ContentBlock();
        contentBlock.setType(blockDTO.getType());
        contentBlock.setOrderIndex(blockDTO.getOrder());
        contentBlock.setData(blockDTO.getData());
        contentBlock.setTitle(blockDTO.getTitle());
        contentBlock.setChapter(chapter);
        return contentBlockRepository.save(contentBlock);
    }

    // Helper method to convert Entity to DTO
    private CourseDTO convertToDTO(Course course) {
        CourseDTO dto = new CourseDTO();
        dto.setId(course.getId());
        dto.setTitle(course.getTitle());
        dto.setDescription(course.getDescription());
        dto.setLevel(course.getLevel());
        dto.setPrice(course.getPrice());
        dto.setStatus(course.getStatus());
        dto.setTrainerId(course.getTrainerId());
        dto.setEnrolledStudents(course.getEnrolledStudents());
        dto.setRating(course.getRating());
        dto.setThumbnail(course.getThumbnail());

        // Set duration
        if (course.getDuration() != null) {
            dto.setDuration(String.valueOf(course.getDuration()));
        } else {
            dto.setDuration("0");
        }

        return dto;
    }

    // Helper method to convert Entity to Summary DTO
    private CourseSummaryDTO convertToSummaryDTO(Course course) {
        CourseSummaryDTO dto = new CourseSummaryDTO();
        dto.setId(course.getId());
        dto.setTitle(course.getTitle());
        dto.setLevel(course.getLevel());
        dto.setPrice(course.getPrice());
        dto.setStatus(course.getStatus());
        dto.setThumbnail(course.getThumbnail());
        dto.setRating(course.getRating());
        dto.setEnrolledStudents(course.getEnrolledStudents());
        return dto;
    }
}