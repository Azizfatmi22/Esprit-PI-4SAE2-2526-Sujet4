package com.example.mscourse.services.impl;

import com.example.mscourse.dto.ContentBlockDTO;
import com.example.mscourse.dto.CreateContentBlockRequestDTO;
import com.example.mscourse.entities.Chapter;
import com.example.mscourse.entities.ContentBlock;
import com.example.mscourse.entities.ContentType;
import com.example.mscourse.exceptions.ResourceNotFoundException;
import com.example.mscourse.repositories.ChapterRepository;
import com.example.mscourse.repositories.ContentBlockRepository;
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
public class ContentBlockServiceImplTest {

    @Mock
    private ContentBlockRepository contentBlockRepository;

    @Mock
    private ChapterRepository chapterRepository;

    @InjectMocks
    private ContentBlockServiceImpl contentBlockService;

    private Chapter chapter;
    private ContentBlock contentBlock;
    private CreateContentBlockRequestDTO createContentBlockDTO;

    @BeforeEach
    void setUp() {
        org.springframework.test.util.ReflectionTestUtils.setField(contentBlockService, "uploadDir", "./uploads");

        chapter = new Chapter();
        chapter.setId(1L);
        
        contentBlock = new ContentBlock();
        contentBlock.setId(1L);
        contentBlock.setTitle("Video Lecture");
        contentBlock.setType(ContentType.VIDEO);
        contentBlock.setChapter(chapter);
        
        createContentBlockDTO = new CreateContentBlockRequestDTO();
        createContentBlockDTO.setTitle("Video Lecture");
        createContentBlockDTO.setType(ContentType.VIDEO);
        createContentBlockDTO.setData("http://example.com/video.mp4");
        createContentBlockDTO.setOrderIndex(1);
    }

    @Test
    void createContentBlock_Success() {
        when(chapterRepository.findById(1L)).thenReturn(Optional.of(chapter));
        when(contentBlockRepository.save(any(ContentBlock.class))).thenReturn(contentBlock);

        ContentBlockDTO result = contentBlockService.createContentBlock(1L, createContentBlockDTO);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Video Lecture", result.getTitle());
        verify(contentBlockRepository).save(any(ContentBlock.class));
    }

    @Test
    void getContentBlockById_Success() {
        when(contentBlockRepository.findById(1L)).thenReturn(Optional.of(contentBlock));

        ContentBlockDTO result = contentBlockService.getContentBlockById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void deleteContentBlock_Success() {
        when(contentBlockRepository.findById(1L)).thenReturn(Optional.of(contentBlock));
        doNothing().when(contentBlockRepository).deleteById(1L);

        assertDoesNotThrow(() -> contentBlockService.deleteContentBlock(1L));
        verify(contentBlockRepository).deleteById(1L);
    }
}
