package com.example.mscourse.services.impl;

import com.example.mscourse.dto.CourseAttachmentDTO;
import com.example.mscourse.dto.CreateAttachmentRequestDTO;
import com.example.mscourse.dto.UpdateAttachmentRequestDTO;
import com.example.mscourse.entities.AttachmentCategory;
import com.example.mscourse.entities.Course;
import com.example.mscourse.entities.CourseAttachment;
import com.example.mscourse.exceptions.ResourceNotFoundException;
import com.example.mscourse.exceptions.ValidationException;
import com.example.mscourse.repositories.CourseAttachmentRepository;
import com.example.mscourse.repositories.CourseRepository;
import com.example.mscourse.services.interfaces.ICourseAttachmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CourseAttachmentServiceImpl implements ICourseAttachmentService {

    private final CourseAttachmentRepository attachmentRepository;
    private final CourseRepository courseRepository;

    @Value("${file.upload.dir:./uploads}")
    private String uploadDir;

    @Override
    public CourseAttachmentDTO createAttachment(Long courseId, CreateAttachmentRequestDTO attachmentDTO) {
        log.info("Creating new attachment for course ID: {}", courseId);

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + courseId));

        CourseAttachment attachment = new CourseAttachment();
        attachment.setFileName(attachmentDTO.getFileName());
        attachment.setFileType(attachmentDTO.getFileType());
        attachment.setFileSize(attachmentDTO.getFileSize());
        attachment.setFileUrl(attachmentDTO.getFileUrl());
        attachment.setCategory(attachmentDTO.getCategory());
        attachment.setDescription(attachmentDTO.getDescription());

        course.addAttachment(attachment);

        CourseAttachment savedAttachment = attachmentRepository.save(attachment);
        log.info("Attachment created successfully with ID: {}", savedAttachment.getId());

        return mapToDTO(savedAttachment);
    }

    @Override
    public CourseAttachmentDTO uploadAttachment(Long courseId, MultipartFile file, AttachmentCategory category, String description) throws IOException {
        log.info("Uploading attachment for course ID: {}", courseId);

        // Validate file
        if (file.isEmpty()) {
            throw new ValidationException("File cannot be empty");
        }

        // Check file size (e.g., max 500MB)
        long maxSize = 500 * 1024 * 1024; // 500MB
        if (file.getSize() > maxSize) {
            throw new ValidationException("File size exceeds maximum allowed size of 500MB");
        }

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + courseId));

        // Get file extension
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename != null ?
                originalFilename.substring(originalFilename.lastIndexOf(".")) : "";
        
        // Filename: {courseId}.{ext}
        String filename = courseId + extension;

        // Create directory structure: uploads/cours_{courseId}/attachments/
        Path uploadPath = Paths.get(uploadDir, "cours_" + courseId, "attachments");
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Save file (will overwrite if exists)
        Path filePath = uploadPath.resolve(filename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        // Create attachment record with proper URL
        CourseAttachment attachment = new CourseAttachment();
        attachment.setFileName(originalFilename);
        attachment.setFileType(file.getContentType());
        attachment.setFileSize(file.getSize());
        attachment.setFileUrl("/api/courses/uploads/cours_" + courseId + "/attachments/" + filename);
        attachment.setCategory(category);
        attachment.setDescription(description);

        course.addAttachment(attachment);

        CourseAttachment savedAttachment = attachmentRepository.save(attachment);
        log.info("Attachment uploaded successfully with ID: {}", savedAttachment.getId());

        return mapToDTO(savedAttachment);
    }

    @Override
    public CourseAttachmentDTO getAttachmentById(Long id) {
        log.info("Fetching attachment by ID: {}", id);
        CourseAttachment attachment = attachmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Attachment not found with id: " + id));
        return mapToDTO(attachment);
    }

    @Override
    public List<CourseAttachmentDTO> getAttachmentsByCourse(Long courseId) {
        log.info("Fetching attachments for course ID: {}", courseId);
        return attachmentRepository.findByCourseId(courseId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<CourseAttachmentDTO> getAttachmentsByCourseAndCategory(Long courseId, AttachmentCategory category) {
        log.info("Fetching attachments for course {} with category: {}", courseId, category);
        return attachmentRepository.findByCourseIdAndCategory(courseId, category).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public CourseAttachmentDTO updateAttachment(Long id, UpdateAttachmentRequestDTO attachmentDTO) {
        log.info("Updating attachment with ID: {}", id);

        CourseAttachment attachment = attachmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Attachment not found with id: " + id));

        if (attachmentDTO.getFileName() != null) {
            attachment.setFileName(attachmentDTO.getFileName());
        }

        if (attachmentDTO.getFileType() != null) {
            attachment.setFileType(attachmentDTO.getFileType());
        }

        if (attachmentDTO.getFileSize() != null) {
            attachment.setFileSize(attachmentDTO.getFileSize());
        }

        if (attachmentDTO.getFileUrl() != null) {
            attachment.setFileUrl(attachmentDTO.getFileUrl());
        }

        if (attachmentDTO.getCategory() != null) {
            attachment.setCategory(attachmentDTO.getCategory());
        }

        if (attachmentDTO.getDescription() != null) {
            attachment.setDescription(attachmentDTO.getDescription());
        }

        CourseAttachment updatedAttachment = attachmentRepository.save(attachment);
        log.info("Attachment updated successfully");

        return mapToDTO(updatedAttachment);
    }

    @Override
    public void deleteAttachment(Long id) {
        log.info("Deleting attachment with ID: {}", id);

        CourseAttachment attachment = attachmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Attachment not found with id: " + id));

        // Delete physical file
        try {
            String fileUrl = attachment.getFileUrl();
            if (fileUrl != null && fileUrl.startsWith("/uploads/")) {
                String filePath = fileUrl.replace("/uploads/", uploadDir + "/");
                Path path = Paths.get(filePath);
                Files.deleteIfExists(path);
            }
        } catch (IOException e) {
            log.warn("Could not delete physical file for attachment: {}", id);
        }

        attachmentRepository.delete(attachment);
        log.info("Attachment deleted successfully");
    }

    @Override
    public void deleteAllAttachmentsByCourse(Long courseId) {
        log.info("Deleting all attachments for course ID: {}", courseId);

        // Get all attachments to delete physical files
        List<CourseAttachment> attachments = attachmentRepository.findByCourseId(courseId);
        for (CourseAttachment attachment : attachments) {
            try {
                String fileUrl = attachment.getFileUrl();
                if (fileUrl != null && fileUrl.startsWith("/uploads/")) {
                    String filePath = fileUrl.replace("/uploads/", uploadDir + "/");
                    Path path = Paths.get(filePath);
                    Files.deleteIfExists(path);
                }
            } catch (IOException e) {
                log.warn("Could not delete physical file for attachment: {}", attachment.getId());
            }
        }

        attachmentRepository.deleteByCourseId(courseId);
        log.info("All attachments deleted successfully");
    }

    @Override
    public byte[] downloadAttachment(Long id) throws IOException {
        log.info("Downloading attachment with ID: {}", id);

        CourseAttachment attachment = attachmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Attachment not found with id: " + id));

        String fileUrl = attachment.getFileUrl();
        if (fileUrl == null || !fileUrl.startsWith("/uploads/")) {
            throw new ValidationException("Attachment file not found");
        }

        String filePath = fileUrl.replace("/uploads/", uploadDir + "/");
        Path path = Paths.get(filePath);

        if (!Files.exists(path)) {
            throw new ResourceNotFoundException("Attachment file not found on disk");
        }

        return Files.readAllBytes(path);
    }

    @Override
    public Long getTotalAttachmentsSize(Long courseId) {
        return attachmentRepository.getTotalSizeByCourseId(courseId);
    }

    @Override
    public List<Object[]> getAttachmentStatistics(Long courseId) {
        return attachmentRepository.getAttachmentStatisticsByCourse(courseId);
    }

    // Mapping methods
    private CourseAttachmentDTO mapToDTO(CourseAttachment attachment) {
        return CourseAttachmentDTO.builder()
                .id(attachment.getId())
                .fileName(attachment.getFileName())
                .fileType(attachment.getFileType())
                .fileSize(attachment.getFileSize())
                .fileUrl(attachment.getFileUrl())
                .category(attachment.getCategory())
                .description(attachment.getDescription())
                .courseId(attachment.getCourse().getId())
                .build();
    }
}