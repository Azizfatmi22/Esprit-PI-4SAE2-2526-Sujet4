package com.example.mscourse.controller;

import com.example.mscourse.dto.ContentBlockDTO;
import com.example.mscourse.entities.ContentType;
import com.example.mscourse.services.interfaces.IContentBlockService;
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
public class ContentBlockControllerTest {

    @Mock
    private IContentBlockService contentBlockService;

    @InjectMocks
    private ContentBlockController contentBlockController;

    private ContentBlockDTO contentBlockDTO;

    @BeforeEach
    void setUp() {
        contentBlockDTO = ContentBlockDTO.builder()
                .id(1L)
                .title("Introduction Video")
                .type(ContentType.VIDEO)
                .chapterId(1L)
                .build();
    }

    @Test
    void getContentBlockById_ReturnsOk() {
        when(contentBlockService.getContentBlockById(1L)).thenReturn(contentBlockDTO);

        ResponseEntity<ContentBlockDTO> response = contentBlockController.getContentBlockById(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Introduction Video", response.getBody().getTitle());
        verify(contentBlockService).getContentBlockById(1L);
    }

    @Test
    void getContentBlocksByChapter_ReturnsOk() {
        when(contentBlockService.getContentBlocksByChapter(1L)).thenReturn(Arrays.asList(contentBlockDTO));

        ResponseEntity<List<ContentBlockDTO>> response = contentBlockController.getContentBlocksByChapter(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        verify(contentBlockService).getContentBlocksByChapter(1L);
    }

    @Test
    void deleteContentBlock_ReturnsNoContent() {
        doNothing().when(contentBlockService).deleteContentBlock(1L);

        ResponseEntity<Void> response = contentBlockController.deleteContentBlock(1L);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(contentBlockService).deleteContentBlock(1L);
    }
}
