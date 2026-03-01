package com.example.mscourse.controller;

import com.example.mscourse.dto.ChapterDTO;
import com.example.mscourse.dto.CreateChapterRequestDTO;
import com.example.mscourse.dto.UpdateChapterRequestDTO;
import com.example.mscourse.services.interfaces.IChapterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/courses/{courseId}/chapters")
@RequiredArgsConstructor
@Slf4j
public class ChapterController {

    private final IChapterService chapterService;

    // ==================== CREATE OPERATIONS ====================

    @PostMapping
    public ResponseEntity<ChapterDTO> createChapter(
            @PathVariable Long courseId,
            @Valid @RequestBody CreateChapterRequestDTO chapterDTO) {
        log.info("REST request to create chapter for course ID: {}", courseId);
        ChapterDTO result = chapterService.createChapter(courseId, chapterDTO);
        return new ResponseEntity<>(result, HttpStatus.CREATED);
    }

    // ==================== READ OPERATIONS ====================

    @GetMapping("/{chapterId}")
    public ResponseEntity<ChapterDTO> getChapterById(@PathVariable Long chapterId) {
        log.info("REST request to get chapter by ID: {}", chapterId);
        ChapterDTO result = chapterService.getChapterById(chapterId);
        return ResponseEntity.ok(result);
    }

    @GetMapping
    public ResponseEntity<List<ChapterDTO>> getChaptersByCourse(@PathVariable Long courseId) {
        log.info("REST request to get chapters for course ID: {}", courseId);
        List<ChapterDTO> result = chapterService.getChaptersByCourse(courseId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{chapterId}/with-content")
    public ResponseEntity<ChapterDTO> getChapterWithContent(@PathVariable Long chapterId) {
        log.info("REST request to get chapter with content by ID: {}", chapterId);
        ChapterDTO result = chapterService.getChapterWithContent(chapterId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/count")
    public ResponseEntity<Integer> getChaptersCount(@PathVariable Long courseId) {
        log.info("REST request to get chapters count for course ID: {}", courseId);
        Integer count = chapterService.getChaptersCountByCourse(courseId);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/{chapterId}/exists")
    public ResponseEntity<Boolean> checkChapterExistsInCourse(
            @PathVariable Long courseId,
            @PathVariable Long chapterId) {
        log.info("REST request to check if chapter {} exists in course {}", chapterId, courseId);
        boolean exists = chapterService.existsInCourse(chapterId, courseId);
        return ResponseEntity.ok(exists);
    }

    // ==================== UPDATE OPERATIONS ====================

    @PutMapping("/{chapterId}")
    public ResponseEntity<ChapterDTO> updateChapter(
            @PathVariable Long chapterId,
            @Valid @RequestBody UpdateChapterRequestDTO chapterDTO) {
        log.info("REST request to update chapter with ID: {}", chapterId);
        ChapterDTO result = chapterService.updateChapter(chapterId, chapterDTO);
        return ResponseEntity.ok(result);
    }

    @PatchMapping("/{chapterId}/reorder")
    public ResponseEntity<ChapterDTO> reorderChapter(
            @PathVariable Long chapterId,
            @RequestParam Integer newOrderIndex) {
        log.info("REST request to reorder chapter {} to position: {}", chapterId, newOrderIndex);
        ChapterDTO result = chapterService.reorderChapter(chapterId, newOrderIndex);
        return ResponseEntity.ok(result);
    }

    // ==================== DELETE OPERATIONS ====================

    @DeleteMapping("/{chapterId}")
    public ResponseEntity<Void> deleteChapter(@PathVariable Long chapterId) {
        log.info("REST request to delete chapter with ID: {}", chapterId);
        chapterService.deleteChapter(chapterId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteAllChapters(@PathVariable Long courseId) {
        log.info("REST request to delete all chapters for course ID: {}", courseId);
        chapterService.deleteAllChaptersByCourse(courseId);
        return ResponseEntity.noContent().build();
    }
}