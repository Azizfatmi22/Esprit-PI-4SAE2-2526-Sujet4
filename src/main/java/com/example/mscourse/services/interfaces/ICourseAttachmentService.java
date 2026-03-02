package com.example.mscourse.services.interfaces;

import com.example.mscourse.dto.CourseAttachmentDTO;
import com.example.mscourse.dto.CreateAttachmentRequestDTO;
import com.example.mscourse.dto.UpdateAttachmentRequestDTO;
import com.example.mscourse.entities.AttachmentCategory;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface ICourseAttachmentService {

    // Create operations
    CourseAttachmentDTO createAttachment(Long courseId, CreateAttachmentRequestDTO attachmentDTO);
    CourseAttachmentDTO uploadAttachment(Long courseId, MultipartFile file, AttachmentCategory category, String description) throws IOException;

    // Read operations
    CourseAttachmentDTO getAttachmentById(Long id);
    List<CourseAttachmentDTO> getAttachmentsByCourse(Long courseId);
    List<CourseAttachmentDTO> getAttachmentsByCourseAndCategory(Long courseId, AttachmentCategory category);

    // Update operations
    CourseAttachmentDTO updateAttachment(Long id, UpdateAttachmentRequestDTO attachmentDTO);
    CourseAttachmentDTO updateAttachmentFile(Long courseId, Long attachmentId, MultipartFile file, AttachmentCategory category, String description) throws IOException;

    // Delete operations
    void deleteAttachment(Long id);
    void deleteAllAttachmentsByCourse(Long courseId);

    // File operations
    byte[] downloadAttachment(Long id) throws IOException;

    // Statistics
    Long getTotalAttachmentsSize(Long courseId);
    List<Object[]> getAttachmentStatistics(Long courseId);
}