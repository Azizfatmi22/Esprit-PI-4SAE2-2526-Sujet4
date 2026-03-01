package com.example.mscourse.services.interfaces;

import com.example.mscourse.dto.ChapterDTO;
import com.example.mscourse.dto.CreateChapterRequestDTO;
import com.example.mscourse.dto.UpdateChapterRequestDTO;

import java.util.List;

public interface IChapterService {

    // Create operations
    ChapterDTO createChapter(Long courseId, CreateChapterRequestDTO chapterDTO);

    // Read operations
    ChapterDTO getChapterById(Long id);
    List<ChapterDTO> getChaptersByCourse(Long courseId);
    ChapterDTO getChapterWithContent(Long id);

    // Update operations
    ChapterDTO updateChapter(Long id, UpdateChapterRequestDTO chapterDTO);
    ChapterDTO reorderChapter(Long id, Integer newOrderIndex);

    // Delete operations
    void deleteChapter(Long id);
    void deleteAllChaptersByCourse(Long courseId);

    // Validation
    boolean existsInCourse(Long chapterId, Long courseId);

    // Statistics
    Integer getChaptersCountByCourse(Long courseId);
}