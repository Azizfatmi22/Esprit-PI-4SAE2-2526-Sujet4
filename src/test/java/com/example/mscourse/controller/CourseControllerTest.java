package com.example.mscourse.controller;

import com.example.mscourse.dto.*;
import com.example.mscourse.entities.Level;
import com.example.mscourse.services.interfaces.IChapterService;
import com.example.mscourse.services.interfaces.ICourseService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseControllerTest {

    @Mock private ICourseService courseService;
    @Mock private IChapterService chapterService;
    @Mock private ObjectMapper objectMapper;

    @InjectMocks
    private CourseController courseController;

    @TempDir
    Path tempDir;

    private CourseDTO courseDTO;
    private CourseSummaryDTO courseSummaryDTO;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(courseController, "fileUploadDir", tempDir.toString());

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

    // ── getCourseTitle ────────────────────────────────────────────────────────

    @Test
    void getCourseTitle_WhenFound_ReturnsOkWithTitle() {
        when(courseService.getCourseById(1L)).thenReturn(courseDTO);

        ResponseEntity<String> response = courseController.getCourseTitle(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Advanced Java", response.getBody());
    }

    @Test
    void getCourseTitle_WhenNotFound_ReturnsNotFound() {
        when(courseService.getCourseById(99L)).thenThrow(new RuntimeException("not found"));

        ResponseEntity<String> response = courseController.getCourseTitle(99L);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("formini_course_not_found", response.getBody());
    }

    // ── createCourse ─────────────────────────────────────────────────────────

    @Test
    void createCourse_WithoutThumbnailOrChapters_ReturnsCreated() {
        when(courseService.createCourse(any(CreateCourseRequestDTO.class))).thenReturn(courseDTO);
        when(courseService.getCourseById(1L)).thenReturn(courseDTO);

        ResponseEntity<CourseDTO> response = courseController.createCourse(
                "Advanced Java", "desc", Level.ADVANCED, 99.99,
                60, "PUBLISHED", "trainer1", null, null);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1L, response.getBody().getId());
        verify(courseService).createCourse(any(CreateCourseRequestDTO.class));
    }

    @Test
    void createCourse_WithThumbnail_SavesFileAndReturnsCreated() throws Exception {
        MockMultipartFile thumbnail = new MockMultipartFile(
                "thumbnail", "thumb.jpg", "image/jpeg", "fake-image-bytes".getBytes());

        when(courseService.createCourse(any(CreateCourseRequestDTO.class))).thenReturn(courseDTO);
        when(courseService.updateCourseThumbnail(eq(1L), anyString())).thenReturn(courseDTO);
        when(courseService.getCourseById(1L)).thenReturn(courseDTO);

        ResponseEntity<CourseDTO> response = courseController.createCourse(
                "Advanced Java", "desc", Level.ADVANCED, 99.99,
                60, "PUBLISHED", "trainer1", thumbnail, null);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        verify(courseService).updateCourseThumbnail(eq(1L), anyString());
    }

    @Test
    void createCourse_WithThumbnailNoExtension_UsesDefaultJpgExtension() throws Exception {
        MockMultipartFile thumbnail = new MockMultipartFile(
                "thumbnail", "thumbnoext", "image/jpeg", "fake-image-bytes".getBytes());

        when(courseService.createCourse(any(CreateCourseRequestDTO.class))).thenReturn(courseDTO);
        when(courseService.updateCourseThumbnail(eq(1L), anyString())).thenReturn(courseDTO);
        when(courseService.getCourseById(1L)).thenReturn(courseDTO);

        ResponseEntity<CourseDTO> response = courseController.createCourse(
                "Advanced Java", "desc", Level.ADVANCED, 99.99,
                null, "DRAFT", "trainer1", thumbnail, null);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        // verify filename ends with .jpg (default extension)
        verify(courseService).updateCourseThumbnail(eq(1L), argThat(name -> name.endsWith(".jpg")));
    }

    @Test
    void createCourse_WithThumbnailNullOriginalFilename_UsesDefaultImageName() throws Exception {
        // MockMultipartFile with empty original name — resolveExtension returns ".jpg"
        // safeOriginal falls back to DEFAULT_IMAGE_NAME = "image"
        MockMultipartFile thumbnail = new MockMultipartFile(
                "thumbnail", "", "image/jpeg", "fake-image-bytes".getBytes());

        when(courseService.createCourse(any(CreateCourseRequestDTO.class))).thenReturn(courseDTO);
        when(courseService.updateCourseThumbnail(eq(1L), anyString())).thenReturn(courseDTO);
        when(courseService.getCourseById(1L)).thenReturn(courseDTO);

        ResponseEntity<CourseDTO> response = courseController.createCourse(
                "Advanced Java", "desc", Level.ADVANCED, 99.99,
                null, "DRAFT", "trainer1", thumbnail, null);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        verify(courseService).updateCourseThumbnail(eq(1L), anyString());
    }

    @Test
    void createCourse_WithChaptersJson_ParsesAndCreatesChapters() throws Exception {
        String chaptersJson = "[{\"title\":\"Chapter 1\",\"description\":\"desc\",\"orderIndex\":1}]";
        ObjectMapper realMapper = new ObjectMapper();
        List<CreateChapterRequestDTO> chapters = realMapper.readValue(chaptersJson,
                new com.fasterxml.jackson.core.type.TypeReference<List<CreateChapterRequestDTO>>() {});

        when(courseService.createCourse(any(CreateCourseRequestDTO.class))).thenReturn(courseDTO);
        when(courseService.getCourseById(1L)).thenReturn(courseDTO);
        when(objectMapper.readValue(eq(chaptersJson),
                any(com.fasterxml.jackson.core.type.TypeReference.class))).thenReturn(chapters);

        ResponseEntity<CourseDTO> response = courseController.createCourse(
                "Advanced Java", "desc", Level.ADVANCED, 99.99,
                60, "PUBLISHED", "trainer1", null, chaptersJson);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        verify(chapterService, atLeastOnce()).createChapter(eq(1L), any(CreateChapterRequestDTO.class));
    }

    @Test
    void createCourse_WithInvalidChaptersJson_LogsErrorAndContinues() throws Exception {
        String badJson = "not-valid-json";
        when(courseService.createCourse(any(CreateCourseRequestDTO.class))).thenReturn(courseDTO);
        when(courseService.getCourseById(1L)).thenReturn(courseDTO);
        when(objectMapper.readValue(eq(badJson),
                any(com.fasterxml.jackson.core.type.TypeReference.class)))
                .thenAnswer(inv -> { throw new RuntimeException("parse error"); });

        ResponseEntity<CourseDTO> response = courseController.createCourse(
                "Advanced Java", "desc", Level.ADVANCED, 99.99,
                60, "PUBLISHED", "trainer1", null, badJson);

        // Should still return CREATED — chapter error is non-fatal
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        verify(chapterService, never()).createChapter(any(), any());
    }

    // ── getCourseById ─────────────────────────────────────────────────────────

    @Test
    void getCourseById_ReturnsOkAndCourse() {
        when(courseService.getCourseById(1L)).thenReturn(courseDTO);

        ResponseEntity<CourseDTO> response = courseController.getCourseById(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Advanced Java", response.getBody().getTitle());
    }

    // ── getCourseWithChapters ─────────────────────────────────────────────────

    @Test
    void getCourseWithChapters_ReturnsOk() {
        when(courseService.getCourseWithChapters(1L)).thenReturn(courseDTO);

        ResponseEntity<CourseDTO> response = courseController.getCourseWithChapters(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(courseService).getCourseWithChapters(1L);
    }

    // ── getAllCourses ─────────────────────────────────────────────────────────

    @Test
    void getAllCourses_ReturnsOkAndPage() {
        Page<CourseSummaryDTO> page = new PageImpl<>(List.of(courseSummaryDTO));
        when(courseService.getAllCourses(any(Pageable.class))).thenReturn(page);

        ResponseEntity<Page<CourseSummaryDTO>> response = courseController.getAllCourses(Pageable.unpaged());

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().getContent().size());
    }

    // ── getCoursesByLevel ─────────────────────────────────────────────────────

    @Test
    void getCoursesByLevel_ReturnsOkAndList() {
        when(courseService.getCoursesByLevel(Level.ADVANCED)).thenReturn(List.of(courseSummaryDTO));

        ResponseEntity<List<CourseSummaryDTO>> response = courseController.getCoursesByLevel(Level.ADVANCED);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    // ── getCoursesByTrainer ───────────────────────────────────────────────────

    @Test
    void getCoursesByTrainer_ReturnsOkAndList() {
        when(courseService.getCoursesByTrainer("trainer1")).thenReturn(List.of(courseSummaryDTO));

        ResponseEntity<List<CourseSummaryDTO>> response = courseController.getCoursesByTrainer("trainer1");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    // ── searchCourses ─────────────────────────────────────────────────────────

    @Test
    void searchCourses_ReturnsOkAndList() {
        when(courseService.searchCourses("java")).thenReturn(List.of(courseSummaryDTO));

        ResponseEntity<List<CourseSummaryDTO>> response = courseController.searchCourses("java");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    // ── getTopRatedCourses ────────────────────────────────────────────────────

    @Test
    void getTopRatedCourses_ReturnsOkAndList() {
        when(courseService.getTopRatedCourses(5)).thenReturn(List.of(courseSummaryDTO));

        ResponseEntity<List<CourseSummaryDTO>> response = courseController.getTopRatedCourses(5);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    // ── getMostEnrolledCourses ────────────────────────────────────────────────

    @Test
    void getMostEnrolledCourses_ReturnsOkAndList() {
        when(courseService.getMostEnrolledCourses(5)).thenReturn(List.of(courseSummaryDTO));

        ResponseEntity<List<CourseSummaryDTO>> response = courseController.getMostEnrolledCourses(5);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    // ── getCourseStatistics ───────────────────────────────────────────────────

    @Test
    void getCourseStatistics_ReturnsOk() {
        CourseStatisticsDTO stats = CourseStatisticsDTO.builder().build();
        when(courseService.getCourseStatistics("trainer1")).thenReturn(stats);

        ResponseEntity<CourseStatisticsDTO> response = courseController.getCourseStatistics("trainer1");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(courseService).getCourseStatistics("trainer1");
    }

    // ── checkCourseExists ─────────────────────────────────────────────────────

    @Test
    void checkCourseExists_WhenExists_ReturnsTrue() {
        when(courseService.existsByTitleAndTrainer("Advanced Java", "trainer1")).thenReturn(true);

        ResponseEntity<Boolean> response = courseController.checkCourseExists("Advanced Java", "trainer1");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody());
    }

    @Test
    void checkCourseExists_WhenNotExists_ReturnsFalse() {
        when(courseService.existsByTitleAndTrainer("Unknown", "trainer1")).thenReturn(false);

        ResponseEntity<Boolean> response = courseController.checkCourseExists("Unknown", "trainer1");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertFalse(response.getBody());
    }

    // ── updateCourseWithFile ──────────────────────────────────────────────────

    @Test
    void updateCourseWithFile_WithoutThumbnail_ReturnsOk() {
        when(courseService.updateCourse(eq(1L), any(UpdateCourseRequestDTO.class))).thenReturn(courseDTO);

        ResponseEntity<CourseDTO> response = courseController.updateCourseWithFile(
                1L, "Updated Title", "desc", Level.ADVANCED, 99.99, 60, "PUBLISHED", null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(courseService).updateCourse(eq(1L), any(UpdateCourseRequestDTO.class));
    }

    @Test
    void updateCourseWithFile_WithThumbnail_SavesFileAndReturnsOk() throws Exception {
        MockMultipartFile thumbnail = new MockMultipartFile(
                "thumbnail", "new-thumb.jpg", "image/jpeg", "bytes".getBytes());

        when(courseService.updateCourse(eq(1L), any(UpdateCourseRequestDTO.class))).thenReturn(courseDTO);

        ResponseEntity<CourseDTO> response = courseController.updateCourseWithFile(
                1L, "Updated Title", "desc", Level.ADVANCED, 99.99, 60, "PUBLISHED", thumbnail);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(courseService).updateCourse(eq(1L), argThat(dto -> dto.getThumbnailUrl() != null));
    }

    // ── updateCourse (JSON) ───────────────────────────────────────────────────

    @Test
    void updateCourse_Json_ReturnsOk() {
        UpdateCourseRequestDTO dto = UpdateCourseRequestDTO.builder()
                .title("Updated").description("desc").level(Level.BEGINNER).price(50.0).build();
        when(courseService.updateCourse(1L, dto)).thenReturn(courseDTO);

        ResponseEntity<CourseDTO> response = courseController.updateCourse(1L, dto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(courseService).updateCourse(1L, dto);
    }

    // ── updateCourseStatus ────────────────────────────────────────────────────

    @Test
    void updateCourseStatus_ReturnsOk() {
        when(courseService.updateCourseStatus(1L, "PUBLISHED")).thenReturn(courseDTO);

        ResponseEntity<CourseDTO> response = courseController.updateCourseStatus(1L, "PUBLISHED");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(courseService).updateCourseStatus(1L, "PUBLISHED");
    }

    // ── updateCourseRating ────────────────────────────────────────────────────

    @Test
    void updateCourseRating_ReturnsOk() {
        when(courseService.updateCourseRating(1L, 4.5)).thenReturn(courseDTO);

        ResponseEntity<CourseDTO> response = courseController.updateCourseRating(1L, 4.5);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(courseService).updateCourseRating(1L, 4.5);
    }

    // ── updateCourseThumbnail ─────────────────────────────────────────────────

    @Test
    void updateCourseThumbnail_SavesFileAndReturnsOk() throws Exception {
        MockMultipartFile thumbnail = new MockMultipartFile(
                "thumbnail", "thumb.png", "image/png", "png-bytes".getBytes());

        when(courseService.updateCourseThumbnail(eq(1L), anyString())).thenReturn(courseDTO);

        ResponseEntity<CourseDTO> response = courseController.updateCourseThumbnail(1L, thumbnail);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(courseService).updateCourseThumbnail(eq(1L), anyString());
    }

    // ── deleteCourse ──────────────────────────────────────────────────────────

    @Test
    void deleteCourse_ReturnsNoContent() {
        doNothing().when(courseService).deleteCourse(1L);

        ResponseEntity<Void> response = courseController.deleteCourse(1L);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(courseService).deleteCourse(1L);
    }
}
