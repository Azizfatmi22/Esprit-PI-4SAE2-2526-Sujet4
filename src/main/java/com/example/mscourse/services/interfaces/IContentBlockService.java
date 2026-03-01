package com.example.mscourse.services.interfaces;

import com.example.mscourse.dto.ContentBlockDTO;
import com.example.mscourse.dto.CreateContentBlockRequestDTO;
import com.example.mscourse.dto.UpdateContentBlockRequestDTO;
import com.example.mscourse.entities.ContentType;

import java.util.List;

public interface IContentBlockService {

    // Create operations
    ContentBlockDTO createContentBlock(Long chapterId, CreateContentBlockRequestDTO contentBlockDTO);

    // Read operations
    ContentBlockDTO getContentBlockById(Long id);
    List<ContentBlockDTO> getContentBlocksByChapter(Long chapterId);
    List<ContentBlockDTO> getContentBlocksByType(Long chapterId, ContentType type);

    // Update operations
    ContentBlockDTO updateContentBlock(Long id, UpdateContentBlockRequestDTO contentBlockDTO);
    ContentBlockDTO reorderContentBlock(Long id, Integer newOrderIndex);

    // Delete operations
    void deleteContentBlock(Long id);
    void deleteAllContentBlocksByChapter(Long chapterId);

    // Search
    List<ContentBlockDTO> searchContentInCourse(Long courseId, String keyword);
    List<ContentBlockDTO> searchContentInChapter(Long chapterId, String keyword);

    // Statistics
    Long countByChapter(Long chapterId);
    List<Object[]> getContentBlockStatistics(Long chapterId);
}