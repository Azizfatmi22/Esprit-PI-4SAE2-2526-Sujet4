package com.example.mscourse.controller;

import com.example.mscourse.dto.CourseAttachmentDTO;
import com.example.mscourse.dto.CreateAttachmentRequestDTO;
import com.example.mscourse.dto.UpdateAttachmentRequestDTO;
import com.example.mscourse.entities.AttachmentCategory;
import com.example.mscourse.services.interfaces.ICourseAttachmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/courses/{courseId}/attachments")
@RequiredArgsConstructor
@Slf4j
public class CourseAttachmentController {

    private final ICourseAttachmentService attachmentService;

    // ==================== CREATE OPERATIONS ====================

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CourseAttachmentDTO> uploadAttachment(
            @PathVariable Long courseId,
            @RequestParam MultipartFile file,
            @RequestParam AttachmentCategory category,
            @RequestParam(required = false) String description) throws IOException {
        log.info("REST request to upload attachment for course ID: {}", courseId);
        CourseAttachmentDTO result = attachmentService.uploadAttachment(courseId, file, category, description);
        return new ResponseEntity<>(result, HttpStatus.CREATED);
    }

    @PostMapping("/metadata")
    public ResponseEntity<CourseAttachmentDTO> createAttachment(
            @PathVariable Long courseId,
            @Valid @RequestBody CreateAttachmentRequestDTO attachmentDTO) {
        log.info("REST request to create attachment metadata for course ID: {}", courseId);
        CourseAttachmentDTO result = attachmentService.createAttachment(courseId, attachmentDTO);
        return new ResponseEntity<>(result, HttpStatus.CREATED);
    }

    // ==================== READ OPERATIONS ====================

    @GetMapping("/{attachmentId}")
    public ResponseEntity<CourseAttachmentDTO> getAttachmentById(@PathVariable Long attachmentId) {
        log.info("REST request to get attachment by ID: {}", attachmentId);
        CourseAttachmentDTO result = attachmentService.getAttachmentById(attachmentId);
        return ResponseEntity.ok(result);
    }

    @GetMapping
    public ResponseEntity<List<CourseAttachmentDTO>> getAttachmentsByCourse(@PathVariable Long courseId) {
        log.info("REST request to get attachments for course ID: {}", courseId);
        List<CourseAttachmentDTO> result = attachmentService.getAttachmentsByCourse(courseId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<List<CourseAttachmentDTO>> getAttachmentsByCourseAndCategory(
            @PathVariable Long courseId,
            @PathVariable AttachmentCategory category) {
        log.info("REST request to get attachments for course {} with category: {}", courseId, category);
        List<CourseAttachmentDTO> result = attachmentService.getAttachmentsByCourseAndCategory(courseId, category);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{attachmentId}/download")
    public ResponseEntity<Resource> downloadAttachment(@PathVariable Long attachmentId) throws IOException {
        log.info("REST request to download attachment with ID: {}", attachmentId);

        CourseAttachmentDTO attachment = attachmentService.getAttachmentById(attachmentId);
        byte[] fileContent = attachmentService.downloadAttachment(attachmentId);

        ByteArrayResource resource = new ByteArrayResource(fileContent);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + attachment.getFileName() + "\"")
                .contentType(MediaType.parseMediaType(attachment.getFileType() != null ?
                        attachment.getFileType() : MediaType.APPLICATION_OCTET_STREAM_VALUE))
                .contentLength(attachment.getFileSize() != null ? attachment.getFileSize() : fileContent.length)
                .body(resource);
    }

    @GetMapping("/statistics")
    public ResponseEntity<List<Object[]>> getAttachmentStatistics(@PathVariable Long courseId) {
        log.info("REST request to get attachment statistics for course ID: {}", courseId);
        List<Object[]> statistics = attachmentService.getAttachmentStatistics(courseId);
        return ResponseEntity.ok(statistics);
    }

    @GetMapping("/total-size")
    public ResponseEntity<Long> getTotalAttachmentsSize(@PathVariable Long courseId) {
        log.info("REST request to get total attachments size for course ID: {}", courseId);
        Long totalSize = attachmentService.getTotalAttachmentsSize(courseId);
        return ResponseEntity.ok(totalSize);
    }

    // ==================== UPDATE OPERATIONS ====================

    @PutMapping("/{attachmentId}")
    public ResponseEntity<CourseAttachmentDTO> updateAttachment(
            @PathVariable Long attachmentId,
            @Valid @RequestBody UpdateAttachmentRequestDTO attachmentDTO) {
        log.info("REST request to update attachment with ID: {}", attachmentId);
        CourseAttachmentDTO result = attachmentService.updateAttachment(attachmentId, attachmentDTO);
        return ResponseEntity.ok(result);
    }

    // ==================== DELETE OPERATIONS ====================

    @DeleteMapping("/{attachmentId}")
    public ResponseEntity<Void> deleteAttachment(@PathVariable Long attachmentId) {
        log.info("REST request to delete attachment with ID: {}", attachmentId);
        attachmentService.deleteAttachment(attachmentId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteAllAttachments(@PathVariable Long courseId) {
        log.info("REST request to delete all attachments for course ID: {}", courseId);
        attachmentService.deleteAllAttachmentsByCourse(courseId);
        return ResponseEntity.noContent().build();
    }
}