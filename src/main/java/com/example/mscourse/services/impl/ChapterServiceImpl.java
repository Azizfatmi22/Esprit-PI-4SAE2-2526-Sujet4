package com.example.mscourse.services.impl;

import com.example.mscourse.dto.ChapterDTO;
import com.example.mscourse.dto.ContentBlockDTO;
import com.example.mscourse.dto.CreateChapterRequestDTO;
import com.example.mscourse.dto.UpdateChapterRequestDTO;
import com.example.mscourse.entities.Chapter;
import com.example.mscourse.entities.Course;
import com.example.mscourse.entities.ContentBlock;
import com.example.mscourse.exceptions.ResourceNotFoundException;
import com.example.mscourse.exceptions.ValidationException;
import com.example.mscourse.repositories.ChapterRepository;
import com.example.mscourse.repositories.CourseRepository;
import com.example.mscourse.services.interfaces.IChapterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ChapterServiceImpl implements IChapterService {

    private final ChapterRepository chapterRepository;
    private final CourseRepository courseRepository;

    public ChapterDTO createChapter(Long courseId, CreateChapterRequestDTO chapterDTO) {
        log.info("Creating new chapter for course ID: {}", courseId);

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + courseId));

        // Set order index if not provided
        if (chapterDTO.getOrderIndex() == null) {
            Integer maxOrderIndex = chapterRepository.getMaxOrderIndex(courseId);
            chapterDTO.setOrderIndex(maxOrderIndex + 1);
        }

        Chapter chapter = new Chapter();
        chapter.setTitle(chapterDTO.getTitle());
        chapter.setDescription(chapterDTO.getDescription());
        chapter.setOrderIndex(chapterDTO.getOrderIndex());

        // ✅ IMPORTANT: Initialiser la liste des contentBlocks
        chapter.setContentBlocks(new ArrayList<>());  // ← Solution 1

        course.addChapter(chapter);

        Chapter savedChapter = chapterRepository.save(chapter);
        log.info("Chapter created successfully with ID: {}", savedChapter.getId());

        return mapToDTO(savedChapter);
    }

    @Override
    public ChapterDTO getChapterById(Long id) {
        log.info("Fetching chapter by ID: {}", id);
        Chapter chapter = chapterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Chapter not found with id: " + id));
        return mapToDTO(chapter);
    }

    @Override
    public List<ChapterDTO> getChaptersByCourse(Long courseId) {
        log.info("Fetching chapters for course ID: {}", courseId);
        return chapterRepository.findByCourseIdOrderByOrderIndexAsc(courseId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public ChapterDTO getChapterWithContent(Long id) {
        log.info("Fetching chapter with content blocks by ID: {}", id);
        Chapter chapter = chapterRepository.findChapterWithContentBlocks(id)
                .orElseThrow(() -> new ResourceNotFoundException("Chapter not found with id: " + id));

        ChapterDTO chapterDTO = mapToDTO(chapter);
        chapterDTO.setContentBlocks(chapter.getContentBlocks().stream()
                .map(this::mapContentBlockToDTO)
                .collect(Collectors.toList()));

        return chapterDTO;
    }

    @Override
    public ChapterDTO updateChapter(Long id, UpdateChapterRequestDTO chapterDTO) {
        log.info("Updating chapter with ID: {}", id);

        Chapter chapter = chapterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Chapter not found with id: " + id));

        if (chapterDTO.getTitle() != null) {
            chapter.setTitle(chapterDTO.getTitle());
        }

        if (chapterDTO.getDescription() != null) {
            chapter.setDescription(chapterDTO.getDescription());
        }

        if (chapterDTO.getOrderIndex() != null) {
            // Handle reordering if needed
            updateChapterOrder(chapter, chapterDTO.getOrderIndex());
        }

        Chapter updatedChapter = chapterRepository.save(chapter);
        log.info("Chapter updated successfully");

        return mapToDTO(updatedChapter);
    }

    @Override
    public ChapterDTO reorderChapter(Long id, Integer newOrderIndex) {
        log.info("Reordering chapter {} to position: {}", id, newOrderIndex);

        Chapter chapter = chapterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Chapter not found with id: " + id));

        updateChapterOrder(chapter, newOrderIndex);

        Chapter updatedChapter = chapterRepository.save(chapter);
        return mapToDTO(updatedChapter);
    }

    @Override
    public void deleteChapter(Long id) {
        log.info("Deleting chapter with ID: {}", id);

        if (!chapterRepository.existsById(id)) {
            throw new ResourceNotFoundException("Chapter not found with id: " + id);
        }

        chapterRepository.deleteById(id);
        log.info("Chapter deleted successfully");
    }

    @Override
    public void deleteAllChaptersByCourse(Long courseId) {
        log.info("Deleting all chapters for course ID: {}", courseId);
        chapterRepository.deleteByCourseId(courseId);
        log.info("All chapters deleted successfully");
    }

    @Override
    public boolean existsInCourse(Long chapterId, Long courseId) {
        return chapterRepository.existsByIdAndCourseId(chapterId, courseId);
    }

    @Override
    public Integer getChaptersCountByCourse(Long courseId) {
        return chapterRepository.countByCourseId(courseId).intValue();
    }

    // Helper methods
    private void updateChapterOrder(Chapter chapter, Integer newOrderIndex) {
        Integer oldOrderIndex = chapter.getOrderIndex();
        Long courseId = chapter.getCourse().getId();

        if (!oldOrderIndex.equals(newOrderIndex)) {
            if (newOrderIndex > oldOrderIndex) {
                // Moving down - shift others up
                chapterRepository.shiftOrderIndices(courseId, oldOrderIndex + 1, -1);
            } else {
                // Moving up - shift others down
                chapterRepository.shiftOrderIndices(courseId, newOrderIndex, 1);
            }
            chapter.setOrderIndex(newOrderIndex);
        }
    }

    // Mapping methods
    private ChapterDTO mapToDTO(Chapter chapter) {
        return ChapterDTO.builder()
                .id(chapter.getId())
                .title(chapter.getTitle())
                .description(chapter.getDescription())
                .orderIndex(chapter.getOrderIndex())
                .courseId(chapter.getCourse().getId())
                .totalContentBlocks(chapter.getContentBlocks() != null ? chapter.getContentBlocks().size() : 0)
                .build();
    }

    private ContentBlockDTO mapContentBlockToDTO(ContentBlock contentBlock) {
        return ContentBlockDTO.builder()
                .id(contentBlock.getId())
                .type(contentBlock.getType())
                .orderIndex(contentBlock.getOrderIndex())
                .data(contentBlock.getData())
                .title(contentBlock.getTitle())
                .chapterId(contentBlock.getChapter().getId())
                .build();
    }
}