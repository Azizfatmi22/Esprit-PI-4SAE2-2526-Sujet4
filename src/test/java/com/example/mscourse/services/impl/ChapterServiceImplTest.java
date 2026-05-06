package com.example.mscourse.services.impl;

import com.example.mscourse.dto.ChapterDTO;
import com.example.mscourse.dto.CreateChapterRequestDTO;
import com.example.mscourse.entities.Chapter;
import com.example.mscourse.entities.Course;
import com.example.mscourse.exceptions.ResourceNotFoundException;
import com.example.mscourse.repositories.ChapterRepository;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ChapterServiceImplTest {

    @Mock
    private ChapterRepository chapterRepository;

    @Mock
    private CourseRepository courseRepository;

    @InjectMocks
    private ChapterServiceImpl chapterService;

    private Course course;
    private Chapter chapter;
    private CreateChapterRequestDTO createDTO;

    @BeforeEach
    void setUp() {
        course = new Course();
        course.setId(1L);
        course.setTitle("Java Course");
        course.setChapters(new java.util.ArrayList<>());

        chapter = new Chapter();
        chapter.setId(1L);
        chapter.setTitle("Introduction");
        chapter.setOrderIndex(1);
        chapter.setCourse(course);

        createDTO = new CreateChapterRequestDTO();
        createDTO.setTitle("Introduction");
        createDTO.setOrderIndex(1);
    }

    @Test
    void createChapter_Success() {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(chapterRepository.save(any(Chapter.class))).thenReturn(chapter);

        ChapterDTO result = chapterService.createChapter(1L, createDTO);

        assertNotNull(result);
        assertEquals("Introduction", result.getTitle());
        verify(chapterRepository).save(any(Chapter.class));
    }

    @Test
    void getChapterById_Success() {
        when(chapterRepository.findById(1L)).thenReturn(Optional.of(chapter));

        ChapterDTO result = chapterService.getChapterById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void getChapterById_NotFound() {
        when(chapterRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> chapterService.getChapterById(1L));
    }

    @Test
    void deleteChapter_Success() {
        when(chapterRepository.findById(1L)).thenReturn(Optional.of(chapter));
        doNothing().when(chapterRepository).delete(any(Chapter.class));

        assertDoesNotThrow(() -> chapterService.deleteChapter(1L));
        verify(chapterRepository).delete(chapter);
    }
}
