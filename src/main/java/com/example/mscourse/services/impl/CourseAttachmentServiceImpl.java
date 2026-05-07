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

    private static final String UPLOADS_API_PATH = "/api/courses/uploads/";
    private static final String ATTACHMENT_NOT_FOUND_MSG = "Attachment not found with id: ";

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

        String originalFilename = validateAndGetFilename(file);

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + courseId));
        
        
        String safeFilename = originalFilename.replaceAll("[^a-zA-Z0-9.-]", "_");

        // Create directory structure: uploads/cours_{courseId}/attachments/
        Path uploadPath = Paths.get(uploadDir, "cours_" + courseId, "attachments");
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Save file with original filename (or append timestamp if exists to avoid overwrite)
        Path filePath = uploadPath.resolve(safeFilename);
        if (Files.exists(filePath)) {
            String nameWithoutExt = safeFilename.substring(0, safeFilename.lastIndexOf("."));
            String extension = safeFilename.substring(safeFilename.lastIndexOf("."));
            safeFilename = nameWithoutExt + "_" + System.currentTimeMillis() + extension;
            filePath = uploadPath.resolve(safeFilename);
        }
        
        try (java.io.InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
        }

        // Create attachment record with proper URL
        CourseAttachment attachment = new CourseAttachment();
        attachment.setFileName(originalFilename);
        attachment.setFileType(file.getContentType());
        attachment.setFileSize(file.getSize());
        attachment.setFileUrl(UPLOADS_API_PATH + "cours_" + courseId + "/attachments/" + safeFilename);
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
                .orElseThrow(() -> new ResourceNotFoundException(ATTACHMENT_NOT_FOUND_MSG + id));
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
                .orElseThrow(() -> new ResourceNotFoundException(ATTACHMENT_NOT_FOUND_MSG + id));

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
    public CourseAttachmentDTO updateAttachmentFile(Long courseId, Long attachmentId, MultipartFile file, AttachmentCategory category, String description) throws IOException {
        log.info("Updating attachment file for ID: {}", attachmentId);

        CourseAttachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new ResourceNotFoundException(ATTACHMENT_NOT_FOUND_MSG + attachmentId));

        if (file != null && !file.isEmpty()) {
            handleFileUpdate(attachment, file, courseId);
        }

        if (category != null) {
            attachment.setCategory(category);
        }

        if (description != null) {
            attachment.setDescription(description);
        }

        CourseAttachment updatedAttachment = attachmentRepository.save(attachment);
        log.info("Attachment file updated successfully");

        return mapToDTO(updatedAttachment);
    }

    private void handleFileUpdate(CourseAttachment attachment, MultipartFile file, Long courseId) throws IOException {
        deletePhysicalFile(attachment.getFileUrl());
        String originalFilename = validateAndGetFilename(file);
        String safeFilename = originalFilename.replaceAll("[^a-zA-Z0-9.-]", "_");
        
        Path uploadPath = Paths.get(uploadDir, "cours_" + courseId, "attachments");
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        Path filePath = uploadPath.resolve(safeFilename);
        if (Files.exists(filePath)) {
            String nameWithoutExt = safeFilename.substring(0, safeFilename.lastIndexOf("."));
            String extension = safeFilename.substring(safeFilename.lastIndexOf("."));
            safeFilename = nameWithoutExt + "_" + System.currentTimeMillis() + extension;
            filePath = uploadPath.resolve(safeFilename);
        }
        
        try (java.io.InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
        }

        attachment.setFileName(originalFilename);
        attachment.setFileType(file.getContentType());
        attachment.setFileSize(file.getSize());
        attachment.setFileUrl(UPLOADS_API_PATH + "cours_" + courseId + "/attachments/" + safeFilename);
        log.info("Updated attachment file: {}", filePath);
    }

    private String validateAndGetFilename(MultipartFile file) {
        validateMultipartFile(file);
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            throw new ValidationException("File name is required");
        }
        return originalFilename;
    }

    private void validateMultipartFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ValidationException("File cannot be empty");
        }
        long maxSize = 500L * 1024 * 1024; // 500MB
        if (file.getSize() > maxSize) {
            throw new ValidationException("File size exceeds maximum allowed size of 500MB");
        }
    }

    private void deletePhysicalFile(String fileUrl) {
        if (fileUrl == null) return;
        try {
            String relativePath = null;
            if (fileUrl.startsWith(UPLOADS_API_PATH)) {
                relativePath = fileUrl.replace(UPLOADS_API_PATH, "");
            } else if (fileUrl.startsWith("/uploads/")) {
                relativePath = fileUrl.replace("/uploads/", "");
            }

            if (relativePath != null) {
                Path path = Paths.get(uploadDir, relativePath);
                Files.deleteIfExists(path);
                log.info("Deleted physical file: {}", path);
            }
        } catch (IOException e) {
            log.warn("Failed to delete physical file: {}", fileUrl, e);
        }
    }

    @Override
    public void deleteAttachment(Long id) {
        log.info("Deleting attachment with ID: {}", id);

        CourseAttachment attachment = attachmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ATTACHMENT_NOT_FOUND_MSG + id));

        deletePhysicalFile(attachment.getFileUrl());

        attachmentRepository.delete(attachment);
        log.info("Attachment deleted successfully");
    }

    @Override
    public void deleteAllAttachmentsByCourse(Long courseId) {
        log.info("Deleting all attachments for course ID: {}", courseId);

        List<CourseAttachment> attachments = attachmentRepository.findByCourseId(courseId);
        for (CourseAttachment attachment : attachments) {
            deletePhysicalFile(attachment.getFileUrl());
        }

        attachmentRepository.deleteByCourseId(courseId);
        log.info("All attachments deleted successfully");
    }

    @Override
    public byte[] downloadAttachment(Long id) throws IOException {
        log.info("Downloading attachment with ID: {}", id);

        CourseAttachment attachment = attachmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ATTACHMENT_NOT_FOUND_MSG + id));

        String fileUrl = attachment.getFileUrl();
        if (fileUrl == null || fileUrl.trim().isEmpty()) {
            throw new ValidationException("Attachment file not found");
        }

        Path path = resolveAttachmentPath(fileUrl);

        if (!Files.exists(path)) {
            throw new ResourceNotFoundException("Attachment file not found on disk");
        }

        return Files.readAllBytes(path);
    }

    private Path resolveAttachmentPath(String fileUrl) {
        String normalized = fileUrl.replace("\\", "/").trim();
        String relativePath;

        if (normalized.startsWith(UPLOADS_API_PATH)) {
            relativePath = normalized.replace(UPLOADS_API_PATH, "");
        } else if (normalized.startsWith("/uploads/")) {
            relativePath = normalized.replace("/uploads/", "");
        } else if (normalized.contains("/uploads/")) {
            relativePath = normalized.substring(normalized.indexOf("/uploads/") + "/uploads/".length());
        } else {
            relativePath = normalized;
        }

        if (relativePath.isEmpty()) {
            throw new ValidationException("Attachment file path is invalid");
        }

        return Paths.get(uploadDir, relativePath).normalize();
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