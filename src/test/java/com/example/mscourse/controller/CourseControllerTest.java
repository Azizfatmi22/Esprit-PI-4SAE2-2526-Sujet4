package com.example.mscourse.controller;

import com.example.mscourse.dto.CourseDTO;
import com.example.mscourse.dto.CourseSummaryDTO;
import com.example.mscourse.entities.Level;
import com.example.mscourse.services.interfaces.ICourseService;
import com.example.mscourse.services.interfaces.IChapterService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CourseControllerTest {

    @Mock
    private ICourseService courseService;

    @Mock
    private IChapterService chapterService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private CourseController courseController;

    private CourseDTO courseDTO;
    private CourseSummaryDTO courseSummaryDTO;

    @BeforeEach
    void setUp() {
        org.springframework.test.util.ReflectionTestUtils.setField(courseController, "fileUploadDir", "./uploads");

        courseDTO = CourseDTO.builder()
                .id(1L)
                .title("Advanced Java")
                .level(Level.ADVANCED)
                .price(99.99)
                .build();

        courseSummaryDTO = CourseSummaryDTO.builder()
                .id(1L)
                .title("Advanced Java")
                .level(Level.ADVANCED)
                .build();
    }

    @Test
    void getCourseById_ReturnsOkAndCourse() {
        when(courseService.getCourseById(1L)).thenReturn(courseDTO);

        ResponseEntity<CourseDTO> response = courseController.getCourseById(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Advanced Java", response.getBody().getTitle());
        verify(courseService).getCourseById(1L);
    }

    @Test
    void getAllCourses_ReturnsOkAndPage() {
        Page<CourseSummaryDTO> page = new PageImpl<>(Arrays.asList(courseSummaryDTO));
        when(courseService.getAllCourses(any(Pageable.class))).thenReturn(page);

        ResponseEntity<Page<CourseSummaryDTO>> response = courseController.getAllCourses(Pageable.unpaged());

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getContent().size());
        verify(courseService).getAllCourses(any(Pageable.class));
    }

    @Test
    void getCoursesByTrainer_ReturnsOkAndList() {
        when(courseService.getCoursesByTrainer("trainer1")).thenReturn(Arrays.asList(courseSummaryDTO));

        ResponseEntity<List<CourseSummaryDTO>> response = courseController.getCoursesByTrainer("trainer1");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        verify(courseService).getCoursesByTrainer("trainer1");
    }

    @Test
    void deleteCourse_ReturnsNoContent() {
        doNothing().when(courseService).deleteCourse(1L);

        ResponseEntity<Void> response = courseController.deleteCourse(1L);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(courseService).deleteCourse(1L);
    }
}
