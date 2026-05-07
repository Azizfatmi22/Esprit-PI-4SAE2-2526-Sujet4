package com.example.mscourse.services.impl;

import com.example.mscourse.dto.ContentBlockDTO;
import com.example.mscourse.dto.CreateContentBlockRequestDTO;
import com.example.mscourse.dto.UpdateContentBlockRequestDTO;
import com.example.mscourse.entities.Chapter;
import com.example.mscourse.entities.ContentBlock;
import com.example.mscourse.entities.ContentType;
import com.example.mscourse.exceptions.ResourceNotFoundException;
import com.example.mscourse.exceptions.ValidationException;
import com.example.mscourse.repositories.ChapterRepository;
import com.example.mscourse.repositories.ContentBlockRepository;
import com.example.mscourse.services.interfaces.IContentBlockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ContentBlockServiceImpl implements IContentBlockService {

    private final ContentBlockRepository contentBlockRepository;
    private final ChapterRepository chapterRepository;

    @Value("${file.upload.dir:./uploads}")
    private String uploadDir;

    private static final String CONTENT_BLOCK_NOT_FOUND_MSG = "Content block not found with id: ";
    private static final String UPLOADS_API_PATH = "/api/courses/uploads/";

    @Override
    public ContentBlockDTO createContentBlock(Long chapterId, CreateContentBlockRequestDTO contentBlockDTO) {
        log.info("Creating new content block for chapter ID: {}", chapterId);

        // Vérifier que le chapitre existe
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new ResourceNotFoundException("Chapter not found with id: " + chapterId));

        // VALIDATION CORRIGÉE - Plus permissive
        validateContentBlockData(contentBlockDTO.getType(), contentBlockDTO.getData());

        // Set order index if not provided
        if (contentBlockDTO.getOrderIndex() == null) {
            Integer maxOrderIndex = contentBlockRepository.getMaxOrderIndex(chapterId);
            contentBlockDTO.setOrderIndex(maxOrderIndex != null ? maxOrderIndex + 1 : 1);
        }

        ContentBlock contentBlock = new ContentBlock();
        contentBlock.setType(contentBlockDTO.getType());
        contentBlock.setOrderIndex(contentBlockDTO.getOrderIndex());
        contentBlock.setData(contentBlockDTO.getData());
        contentBlock.setTitle(contentBlockDTO.getTitle());

        chapter.addContentBlock(contentBlock);

        ContentBlock savedContentBlock = contentBlockRepository.save(contentBlock);
        log.info("Content block created successfully with ID: {}", savedContentBlock.getId());

        return mapToDTO(savedContentBlock);
    }

    @Override
    public ContentBlockDTO getContentBlockById(Long id) {
        log.info("Fetching content block by ID: {}", id);
        ContentBlock contentBlock = contentBlockRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(CONTENT_BLOCK_NOT_FOUND_MSG + id));
        return mapToDTO(contentBlock);
    }

    @Override
    public List<ContentBlockDTO> getContentBlocksByChapter(Long chapterId) {
        log.info("Fetching content blocks for chapter ID: {}", chapterId);
        return contentBlockRepository.findByChapterIdOrderByOrderIndexAsc(chapterId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<ContentBlockDTO> getContentBlocksByType(Long chapterId, ContentType type) {
        log.info("Fetching content blocks of type {} for chapter ID: {}", type, chapterId);
        return contentBlockRepository.findByChapterIdAndType(chapterId, type).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public ContentBlockDTO updateContentBlock(Long id, UpdateContentBlockRequestDTO contentBlockDTO) {
        log.info("Updating content block with ID: {}", id);

        ContentBlock contentBlock = contentBlockRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(CONTENT_BLOCK_NOT_FOUND_MSG + id));

        if (contentBlockDTO.getType() != null) {
            contentBlock.setType(contentBlockDTO.getType());
        }

        if (contentBlockDTO.getData() != null) {
            // Validate data for the content type
            validateContentBlockData(
                    contentBlockDTO.getType() != null ? contentBlockDTO.getType() : contentBlock.getType(),
                    contentBlockDTO.getData()
            );
            contentBlock.setData(contentBlockDTO.getData());
        }

        if (contentBlockDTO.getTitle() != null) {
            contentBlock.setTitle(contentBlockDTO.getTitle());
        }

        if (contentBlockDTO.getOrderIndex() != null) {
            updateContentBlockOrder(contentBlock, contentBlockDTO.getOrderIndex());
        }

        ContentBlock updatedContentBlock = contentBlockRepository.save(contentBlock);
        log.info("Content block updated successfully");

        return mapToDTO(updatedContentBlock);
    }

    @Override
    public ContentBlockDTO reorderContentBlock(Long id, Integer newOrderIndex) {
        log.info("Reordering content block {} to position: {}", id, newOrderIndex);

        ContentBlock contentBlock = contentBlockRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(CONTENT_BLOCK_NOT_FOUND_MSG + id));

        updateContentBlockOrder(contentBlock, newOrderIndex);

        ContentBlock updatedContentBlock = contentBlockRepository.save(contentBlock);
        return mapToDTO(updatedContentBlock);
    }

    @Override
    public void deleteContentBlock(Long id) {
        log.info("Deleting content block with ID: {}", id);

        ContentBlock contentBlock = contentBlockRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(CONTENT_BLOCK_NOT_FOUND_MSG + id));

        // Delete physical file if it exists
        try {
            String fileUrl = contentBlock.getData();
            if (fileUrl != null && fileUrl.startsWith(UPLOADS_API_PATH)) {
                // Extract the relative path from the URL
                String relativePath = fileUrl.replace(UPLOADS_API_PATH, "");
                Path filePath = Paths.get(uploadDir, relativePath);
                if (Files.exists(filePath)) {
                    Files.delete(filePath);
                    log.info("Deleted content block file: {}", filePath);
                }
            }
        } catch (IOException e) {
            log.warn("Failed to delete content block file", e);
        }

        contentBlockRepository.deleteById(id);
        log.info("Content block deleted successfully");
    }

    @Override
    public void deleteAllContentBlocksByChapter(Long chapterId) {
        log.info("Deleting all content blocks for chapter ID: {}", chapterId);
        contentBlockRepository.deleteByChapterId(chapterId);
        log.info("All content blocks deleted successfully");
    }

    @Override
    public List<ContentBlockDTO> searchContentInCourse(Long courseId, String keyword) {
        log.info("Searching content in course {} with keyword: {}", courseId, keyword);
        return contentBlockRepository.searchContentByCourseId(courseId, keyword).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<ContentBlockDTO> searchContentInChapter(Long chapterId, String keyword) {
        log.info("Searching content in chapter {} with keyword: {}", chapterId, keyword);
        return contentBlockRepository.searchContentByChapterId(chapterId, keyword).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public Long countByChapter(Long chapterId) {
        return contentBlockRepository.countByChapterId(chapterId);
    }

    @Override
    public List<Object[]> getContentBlockStatistics(Long chapterId) {
        return contentBlockRepository.getContentBlockStatistics(chapterId);
    }

    /**
     * VALIDATION CORRIGÉE - Accepte les chemins /api/uploads/ et /api/courses/uploads/ avec content_block_id
     */
    private void validateContentBlockData(ContentType type, String data) {
        if (data == null || data.trim().isEmpty()) {
            throw new ValidationException("Content data cannot be empty");
        }

        switch (type) {
            case TEXT:
                // Text can be any content
                break;

            case IMAGE:
            case VIDEO:
            case PDF:
            case FILE:
                // Accept various URL formats from file upload
                // Including new structure: /api/courses/uploads/cours_{id}/chapitre_{id}/content_block_{id}/{type}/filename
                if (!data.startsWith(UPLOADS_API_PATH) && 
                    !data.startsWith("/api/uploads/") && 
                    !data.startsWith("http") &&
                    !data.equals("temp")) { // Allow temp placeholder during creation
                    throw new ValidationException(
                            "For media content, data must be a valid URL or server path. " +
                                    "Received: " + data + ". Expected: starts with " + UPLOADS_API_PATH + ", /api/uploads/ or http"
                    );
                }
                log.debug("Media URL accepted: {}", data);
                break;

            case QUIZ:
            case ASSIGNMENT:
                // Pour quiz et assignment, on peut avoir du JSON
                if (!data.trim().startsWith("{") && !data.trim().startsWith("[")) {
                    log.warn("Quiz/Assignment data might not be valid JSON: {}", data);
                }
                break;

            default:
                log.warn("Unknown content type: {}", type);
        }
    }

    private void updateContentBlockOrder(ContentBlock contentBlock, Integer newOrderIndex) {
        Integer oldOrderIndex = contentBlock.getOrderIndex();
        Long chapterId = contentBlock.getChapter().getId();

        if (!oldOrderIndex.equals(newOrderIndex)) {
            if (newOrderIndex > oldOrderIndex) {
                // Moving down - shift others up
                contentBlockRepository.shiftOrderIndices(chapterId, oldOrderIndex + 1, newOrderIndex, -1);
            } else {
                // Moving up - shift others down
                contentBlockRepository.shiftOrderIndices(chapterId, newOrderIndex, oldOrderIndex - 1, 1);
            }
            contentBlock.setOrderIndex(newOrderIndex);
        }
    }

    private ContentBlockDTO mapToDTO(ContentBlock contentBlock) {
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