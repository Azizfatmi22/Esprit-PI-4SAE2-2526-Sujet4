package com.example.mscourse.services.impl;

import com.example.mscourse.dto.CourseDTO;
import com.example.mscourse.dto.CreateCourseRequestDTO;
import com.example.mscourse.entities.Course;
import com.example.mscourse.entities.Level;
import com.example.mscourse.exceptions.ResourceNotFoundException;
import com.example.mscourse.exceptions.ValidationException;
import com.example.mscourse.repositories.CourseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
        
        createCourseDTO = new CreateCourseRequestDTO();
        createCourseDTO.setTitle("Java Programming");
        createCourseDTO.setTrainerId("trainer-1");
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
    void getAllCourses_ReturnsPage() {
        org.springframework.data.domain.Page<Course> page = new org.springframework.data.domain.PageImpl<>(java.util.List.of(course));
        when(courseRepository.findAll(any(org.springframework.data.domain.Pageable.class))).thenReturn(page);

        org.springframework.data.domain.Page<com.example.mscourse.dto.CourseSummaryDTO> result = courseService.getAllCourses(org.springframework.data.domain.PageRequest.of(0, 10));

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("Java Programming", result.getContent().get(0).getTitle());
    }

    @Test
    void updateCourse_Success() {
        com.example.mscourse.dto.UpdateCourseRequestDTO updateDTO = new com.example.mscourse.dto.UpdateCourseRequestDTO();
        updateDTO.setTitle("Updated Title");

        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CourseDTO result = courseService.updateCourse(1L, updateDTO);

        assertNotNull(result);
        assertEquals("Updated Title", result.getTitle());
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
}
