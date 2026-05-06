package com.example.mscourse.controller;

import com.example.mscourse.dto.ChapterDTO;
import com.example.mscourse.services.interfaces.IChapterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ChapterControllerTest {

    @Mock
    private IChapterService chapterService;

    @InjectMocks
    private ChapterController chapterController;

    private ChapterDTO chapterDTO;

    @BeforeEach
    void setUp() {
        chapterDTO = ChapterDTO.builder()
                .id(1L)
                .title("Introduction")
                .courseId(1L)
                .orderIndex(1)
                .build();
    }

    @Test
    void getChapterById_ReturnsOk() {
        when(chapterService.getChapterById(1L)).thenReturn(chapterDTO);

        ResponseEntity<ChapterDTO> response = chapterController.getChapterById(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Introduction", response.getBody().getTitle());
        verify(chapterService).getChapterById(1L);
    }

    @Test
    void getChaptersByCourse_ReturnsOk() {
        when(chapterService.getChaptersByCourse(1L)).thenReturn(Arrays.asList(chapterDTO));

        ResponseEntity<List<ChapterDTO>> response = chapterController.getChaptersByCourse(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        verify(chapterService).getChaptersByCourse(1L);
    }

    @Test
    void deleteChapter_ReturnsNoContent() {
        doNothing().when(chapterService).deleteChapter(1L);

        ResponseEntity<Void> response = chapterController.deleteChapter(1L);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(chapterService).deleteChapter(1L);
    }
}
