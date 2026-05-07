package com.example.mscourse.services.impl;

import com.example.mscourse.dto.*;
import com.example.mscourse.entities.*;
import com.example.mscourse.exceptions.ResourceNotFoundException;
import com.example.mscourse.exceptions.ValidationException;
import com.example.mscourse.repositories.CourseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CourseServiceImplTest {

    @Mock
    private CourseRepository courseRepository;

    @InjectMocks
    private CourseServiceImpl courseService;

    private Course course;
    private CreateCourseRequestDTO createCourseDTO;

    @BeforeEach
    void setUp() {
        course = new Course();
        course.setId(1L);
        course.setTitle("Java Programming");
        course.setTrainerId("trainer-1");
        course.setLevel(Level.BEGINNER);
        course.setStatus("DRAFT");
        course.setEnrolledStudents(10);
        course.setRating(4.5);
        course.setChapters(new ArrayList<>());
        course.setAttachments(new ArrayList<>());
        
        createCourseDTO = new CreateCourseRequestDTO();
        createCourseDTO.setTitle("Java Programming");
        createCourseDTO.setTrainerId("trainer-1");
        createCourseDTO.setLevel(Level.BEGINNER);
        createCourseDTO.setPrice(99.99);
    }

    @Test
    void createCourse_Success() {
        when(courseRepository.existsByTitleAndTrainerId(anyString(), anyString())).thenReturn(false);
        when(courseRepository.save(any(Course.class))).thenReturn(course);

        CourseDTO result = courseService.createCourse(createCourseDTO);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Java Programming", result.getTitle());
        verify(courseRepository).save(any(Course.class));
    }

    @Test
    void createCourse_ThrowsValidationException_WhenTitleExistsForTrainer() {
        when(courseRepository.existsByTitleAndTrainerId(anyString(), anyString())).thenReturn(true);

        assertThrows(ValidationException.class, () -> courseService.createCourse(createCourseDTO));
        verify(courseRepository, never()).save(any(Course.class));
    }

    @Test
    void getCourseById_Success() {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));

        CourseDTO result = courseService.getCourseById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void getCourseById_ThrowsResourceNotFoundException() {
        when(courseRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> courseService.getCourseById(1L));
    }

    @Test
    void getCourseWithChapters_Success() {
        Chapter chapter = new Chapter();
        chapter.setId(10L);
        chapter.setTitle("Intro");
        chapter.setOrderIndex(1);
        chapter.setCourse(course);
        course.getChapters().add(chapter);

        ContentBlock cb = new ContentBlock();
        cb.setId(100L);
        cb.setTitle("Welcome");
        cb.setOrderIndex(1);
        cb.setChapter(chapter);
        chapter.setContentBlocks(List.of(cb));

        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));

        CourseDTO result = courseService.getCourseWithChapters(1L);

        assertNotNull(result);
        assertEquals(1, result.getChapters().size());
        assertEquals(1, result.getChapters().get(0).getContentBlocks().size());
    }

    @Test
    void getAllCourses_ReturnsPage() {
        Page<Course> page = new PageImpl<>(List.of(course));
        when(courseRepository.findAll(any(Pageable.class))).thenReturn(page);

        Page<CourseSummaryDTO> result = courseService.getAllCourses(PageRequest.of(0, 10));

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("Java Programming", result.getContent().get(0).getTitle());
    }

    @Test
    void getCoursesByLevel_Success() {
        when(courseRepository.findByLevel(Level.BEGINNER)).thenReturn(List.of(course));
        List<CourseSummaryDTO> result = courseService.getCoursesByLevel(Level.BEGINNER);
        assertEquals(1, result.size());
    }

    @Test
    void getCoursesByTrainer_Success() {
        when(courseRepository.findByTrainerId("trainer-1")).thenReturn(List.of(course));
        List<CourseSummaryDTO> result = courseService.getCoursesByTrainer("trainer-1");
        assertEquals(1, result.size());
    }

    @Test
    void searchCourses_Success() {
        when(courseRepository.searchCourses("java")).thenReturn(List.of(course));
        List<CourseSummaryDTO> result = courseService.searchCourses("java");
        assertEquals(1, result.size());
    }

    @Test
    void updateCourse_Success() {
        UpdateCourseRequestDTO updateDTO = new UpdateCourseRequestDTO();
        updateDTO.setTitle("Updated Title");
        updateDTO.setDescription("New Desc");
        updateDTO.setPrice(49.99);

        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CourseDTO result = courseService.updateCourse(1L, updateDTO);

        assertNotNull(result);
        assertEquals("Updated Title", result.getTitle());
        assertEquals("New Desc", result.getDescription());
    }

    @Test
    void updateCourse_DuplicateTitle_ThrowsException() {
        UpdateCourseRequestDTO updateDTO = new UpdateCourseRequestDTO();
        updateDTO.setTitle("Other Course");

        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(courseRepository.existsByTitleAndTrainerId("Other Course", "trainer-1")).thenReturn(true);

        assertThrows(ValidationException.class, () -> courseService.updateCourse(1L, updateDTO));
    }

    @Test
    void updateCourseStatus_Success() {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CourseDTO result = courseService.updateCourseStatus(1L, "PUBLISHED");

        assertNotNull(result);
        assertEquals("PUBLISHED", result.getStatus());
    }

    @Test
    void updateCourseRating_Success() {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(courseRepository.save(any(Course.class))).thenAnswer(i -> i.getArgument(0));
        
        CourseDTO result = courseService.updateCourseRating(1L, 4.0);
        assertEquals(4.0, result.getRating());
    }

    @Test
    void updateCourseRating_Invalid_ThrowsException() {
        assertThrows(ValidationException.class, () -> courseService.updateCourseRating(1L, 6.0));
    }

    @Test
    void updateCourseThumbnail_Success() {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(courseRepository.save(any(Course.class))).thenAnswer(i -> i.getArgument(0));
        
        CourseDTO result = courseService.updateCourseThumbnail(1L, "new-thumb.jpg");
        assertEquals("new-thumb.jpg", result.getThumbnailUrl());
    }

    @Test
    void deleteCourse_Success() {
        when(courseRepository.existsById(1L)).thenReturn(true);
        doNothing().when(courseRepository).deleteById(1L);

        assertDoesNotThrow(() -> courseService.deleteCourse(1L));
        verify(courseRepository).deleteById(1L);
    }

    @Test
    void deleteCourse_ThrowsResourceNotFoundException() {
        when(courseRepository.existsById(1L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> courseService.deleteCourse(1L));
        verify(courseRepository, never()).deleteById(anyLong());
    }

    @Test
    void getCourseStatistics_Success() {
        when(courseRepository.findByTrainerId("trainer-1")).thenReturn(List.of(course));
        
        CourseStatisticsDTO stats = courseService.getCourseStatistics("trainer-1");
        
        assertNotNull(stats);
        assertEquals(1L, stats.getTotalCourses());
        assertEquals(10L, stats.getTotalEnrollments());
        assertEquals(4.5, stats.getAverageRating());
    }

    @Test
    void getTopRatedCourses_Success() {
        when(courseRepository.findTop10ByOrderByCreatedAtDesc()).thenReturn(List.of(course));
        List<CourseSummaryDTO> result = courseService.getTopRatedCourses(1);
        assertEquals(1, result.size());
    }

    @Test
    void getMostEnrolledCourses_Success() {
        when(courseRepository.findAll()).thenReturn(List.of(course));
        List<CourseSummaryDTO> result = courseService.getMostEnrolledCourses(1);
        assertEquals(1, result.size());
    }
}
