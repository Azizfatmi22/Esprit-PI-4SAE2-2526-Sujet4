package com.example.mscourse.controller;

import com.example.mscourse.dto.ContentBlockDTO;
import com.example.mscourse.dto.CreateContentBlockRequestDTO;
import com.example.mscourse.dto.UpdateContentBlockRequestDTO;
import com.example.mscourse.entities.ContentType;
import com.example.mscourse.services.interfaces.IContentBlockService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/courses/chapters/{chapterId}/content-blocks")
@RequiredArgsConstructor
@Slf4j
public class ContentBlockController {

    private final IContentBlockService contentBlockService;

    // ==================== CREATE OPERATIONS ====================

    @PostMapping
    public ResponseEntity<ContentBlockDTO> createContentBlock(
            @PathVariable Long chapterId,
            @Valid @RequestBody CreateContentBlockRequestDTO contentBlockDTO) {
        log.info("REST request to create content block for chapter ID: {}", chapterId);
        ContentBlockDTO result = contentBlockService.createContentBlock(chapterId, contentBlockDTO);
        return new ResponseEntity<>(result, HttpStatus.CREATED);
    }

    // ==================== READ OPERATIONS ====================

    @GetMapping("/{contentBlockId}")
    public ResponseEntity<ContentBlockDTO> getContentBlockById(@PathVariable Long contentBlockId) {
        log.info("REST request to get content block by ID: {}", contentBlockId);
        ContentBlockDTO result = contentBlockService.getContentBlockById(contentBlockId);
        return ResponseEntity.ok(result);
    }

    @GetMapping
    public ResponseEntity<List<ContentBlockDTO>> getContentBlocksByChapter(@PathVariable Long chapterId) {
        log.info("REST request to get content blocks for chapter ID: {}", chapterId);
        List<ContentBlockDTO> result = contentBlockService.getContentBlocksByChapter(chapterId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/type/{type}")
    public ResponseEntity<List<ContentBlockDTO>> getContentBlocksByType(
            @PathVariable Long chapterId,
            @PathVariable ContentType type) {
        log.info("REST request to get content blocks of type {} for chapter ID: {}", type, chapterId);
        List<ContentBlockDTO> result = contentBlockService.getContentBlocksByType(chapterId, type);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/search")
    public ResponseEntity<List<ContentBlockDTO>> searchContentInChapter(
            @PathVariable Long chapterId,
            @RequestParam String keyword) {
        log.info("REST request to search content in chapter {} with keyword: {}", chapterId, keyword);
        List<ContentBlockDTO> result = contentBlockService.searchContentInChapter(chapterId, keyword);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/course/{courseId}/search")
    public ResponseEntity<List<ContentBlockDTO>> searchContentInCourse(
            @PathVariable Long courseId,
            @RequestParam String keyword) {
        log.info("REST request to search content in course {} with keyword: {}", courseId, keyword);
        List<ContentBlockDTO> result = contentBlockService.searchContentInCourse(courseId, keyword);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/count")
    public ResponseEntity<Long> getContentBlocksCount(@PathVariable Long chapterId) {
        log.info("REST request to get content blocks count for chapter ID: {}", chapterId);
        Long count = contentBlockService.countByChapter(chapterId);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/statistics")
    public ResponseEntity<List<Object[]>> getContentBlockStatistics(@PathVariable Long chapterId) {
        log.info("REST request to get content block statistics for chapter ID: {}", chapterId);
        List<Object[]> statistics = contentBlockService.getContentBlockStatistics(chapterId);
        return ResponseEntity.ok(statistics);
    }

    // ==================== UPDATE OPERATIONS ====================

    @PutMapping("/{contentBlockId}")
    public ResponseEntity<ContentBlockDTO> updateContentBlock(
            @PathVariable Long contentBlockId,
            @Valid @RequestBody UpdateContentBlockRequestDTO contentBlockDTO) {
        log.info("REST request to update content block with ID: {}", contentBlockId);
        ContentBlockDTO result = contentBlockService.updateContentBlock(contentBlockId, contentBlockDTO);
        return ResponseEntity.ok(result);
    }

    @PatchMapping("/{contentBlockId}/reorder")
    public ResponseEntity<ContentBlockDTO> reorderContentBlock(
            @PathVariable Long contentBlockId,
            @RequestParam Integer newOrderIndex) {
        log.info("REST request to reorder content block {} to position: {}", contentBlockId, newOrderIndex);
        ContentBlockDTO result = contentBlockService.reorderContentBlock(contentBlockId, newOrderIndex);
        return ResponseEntity.ok(result);
    }

    // ==================== DELETE OPERATIONS ====================

    @DeleteMapping("/{contentBlockId}")
    public ResponseEntity<Void> deleteContentBlock(@PathVariable Long contentBlockId) {
        log.info("REST request to delete content block with ID: {}", contentBlockId);
        contentBlockService.deleteContentBlock(contentBlockId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteAllContentBlocks(@PathVariable Long chapterId) {
        log.info("REST request to delete all content blocks for chapter ID: {}", chapterId);
        contentBlockService.deleteAllContentBlocksByChapter(chapterId);
        return ResponseEntity.noContent().build();
    }
}