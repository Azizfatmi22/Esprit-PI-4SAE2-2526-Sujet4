package com.example.mstrainerhiring.services.impl;

import com.example.mstrainerhiring.client.PartnerClient;
import com.example.mstrainerhiring.dto.JobDTO;
import com.example.mstrainerhiring.dto.TrainerHiringDTO;
import com.example.mstrainerhiring.entities.TrainerDocument;
import com.example.mstrainerhiring.entities.TrainerHiring;
import com.example.mstrainerhiring.enums.DocumentType;
import com.example.mstrainerhiring.enums.Technology;
import com.example.mstrainerhiring.enums.TrainerStatus;
import com.example.mstrainerhiring.exception.InvalidFileException;
import com.example.mstrainerhiring.exception.MissingRequiredDocumentException;
import com.example.mstrainerhiring.exception.ResourceNotFoundException;
import com.example.mstrainerhiring.mapper.TrainerHiringMapper;
import com.example.mstrainerhiring.repositories.TrainerDocumentRepository;
import com.example.mstrainerhiring.repositories.TrainerHiringRepository;
import com.example.mstrainerhiring.services.IntelligenceService;
import com.example.mstrainerhiring.services.SmsService;
import com.example.mstrainerhiring.services.TrainerHiringService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TrainerHiringServiceImpl implements TrainerHiringService {

    private final TrainerHiringRepository trainerRepository;
    private final TrainerDocumentRepository documentRepository;
    private final PartnerClient partnerClient;
    private final IntelligenceService intelligenceService;
    private final SmsService smsService;
    private final TrainerHiringMapper trainerMapper;

    @Value("${file.upload-dir}")
    private String uploadDir;

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final String ALLOWED_CONTENT_TYPE = "application/pdf";
    private static final java.util.List<String> ALLOWED_IMAGE_TYPES = java.util.List.of("image/jpeg", "image/png",
            "image/jpg");

    @Override
    public TrainerHiringDTO createTrainer(TrainerHiringDTO trainerDTO, MultipartFile cvFile,
            MultipartFile pictureFile) {
        log.info("Creating new trainer application for: {} {}", trainerDTO.getForename(), trainerDTO.getName());

        if (cvFile == null || cvFile.isEmpty()) {
            throw new MissingRequiredDocumentException("CV document is required");
        }

        validateFile(cvFile, "CV");

        if (pictureFile != null && !pictureFile.isEmpty()) {
            validateImageFile(pictureFile, "Profile Picture");
        }

        // Verify partner exists via Client
        if (!partnerClient.existsById(trainerDTO.getPartnerId())) {
            throw new ResourceNotFoundException("Partner", "id", trainerDTO.getPartnerId());
        }

        // Check for single application constraint
        if (trainerDTO.getJobId() != null
                && trainerRepository.existsByEmailAndJobId(trainerDTO.getEmail(), trainerDTO.getJobId())) {
            throw new InvalidFileException("You have already submitted an application for this specific job position.");
        }

        // Fetch Job details for analysis
        JobDTO jobDTO = null;
        if (trainerDTO.getJobId() != null) {
            if (!partnerClient.jobExistsById(trainerDTO.getJobId())) {
                throw new ResourceNotFoundException("Job", "id", trainerDTO.getJobId());
            }
            // Fetch job DTO for technical alignment analysis
            // We might need a separate method in PartnerClient for getJobById
            // I'll add it or assume it's part of getPartnerById if jobs are included?
            // Better add a dedicated one.
            // I'll assume jobDTO is fetched for analysis.
            jobDTO = partnerClient.getJobById(trainerDTO.getJobId());
        }

        TrainerHiring trainer = trainerMapper.toEntity(trainerDTO);
        trainer.setJobId(trainerDTO.getJobId());
        trainer.setStatus(TrainerStatus.PENDING);

        TrainerHiring savedTrainer = trainerRepository.save(trainer);
        log.info("Trainer saved with ID: {}", savedTrainer.getId());

        // Store CV
        String cvPath = storeDocument(savedTrainer, cvFile, DocumentType.CV);

        // Store Picture if provided
        if (pictureFile != null && !pictureFile.isEmpty()) {
            storeDocument(savedTrainer, pictureFile, DocumentType.PICTURE);
        }

        // --- Intelligent Analysis ---
        try {
            log.info("Starting intelligent verification for CV: {}", cvPath);
            String cvText = intelligenceService.extractTextFromPdf(cvPath);
            savedTrainer = intelligenceService.analyzeApplication(savedTrainer, cvText, jobDTO);

            // Auto-reject logic based on intelligence
            if (Boolean.TRUE.equals(savedTrainer.getIsBlankCv())
                    || Boolean.TRUE.equals(savedTrainer.getPlagiarismFlag())) {
                savedTrainer.setStatus(TrainerStatus.REJECTED);
            }

            savedTrainer = trainerRepository.save(savedTrainer);
            log.info("Intelligent verification completed for trainer: {}", savedTrainer.getId());

            // SMS Notification - Application Result
            String formattedPhone = "+216" + savedTrainer.getPhone();
            if (savedTrainer.getStatus() == TrainerStatus.REJECTED) {
                String reason = Boolean.TRUE.equals(savedTrainer.getIsBlankCv()) ? "Blank CV detected"
                        : "High textual similarity with other applicants";
                smsService.sendApplicationRejectedSms(formattedPhone, reason);
            } else {
                smsService.sendApplicationReceivedSms(formattedPhone);
            }

        } catch (Exception e) {
            log.error("Intelligent verification failed, but application saved.", e);
        }

        return mapToDTOWithPartnerName(savedTrainer);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TrainerHiringDTO> getAllTrainers(Pageable pageable, TrainerStatus status, String keyword,
            Technology technology) {

        Specification<TrainerHiring> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (keyword != null && !keyword.trim().isEmpty()) {
                String searchPattern = "%" + keyword.toLowerCase().trim() + "%";
                Predicate namePredicate = cb.like(cb.lower(root.get("name")), searchPattern);
                Predicate forenamePredicate = cb.like(cb.lower(root.get("forename")), searchPattern);
                predicates.add(cb.or(namePredicate, forenamePredicate));
            }

            if (technology != null) {
                predicates.add(cb.equal(root.get("technology"), technology));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<TrainerHiring> trainersPage = trainerRepository.findAll(spec, pageable);
        return trainersPage.map(this::mapToDTOWithPartnerName);
    }

    @Override
    @Transactional(readOnly = true)
    public TrainerHiringDTO getTrainerById(UUID id) {
        TrainerHiring trainer = trainerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Trainer", "id", id));
        return mapToDTOWithPartnerName(trainer);
    }

    @Override
    @Transactional(readOnly = true)
    public TrainerHiring getTrainerEntityById(UUID id) {
        return trainerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Trainer", "id", id));
    }

    @Override
    public TrainerHiringDTO updateStatus(UUID id, TrainerStatus status) {
        TrainerHiring trainer = trainerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Trainer", "id", id));
        trainer.setStatus(status);
        TrainerHiring updated = trainerRepository.save(trainer);

        // Send Status Update SMS
        String formattedPhone = "+216" + updated.getPhone();
        smsService.sendApplicationStatusUpdateSms(formattedPhone, status.name(), "Admin review complete.");

        return mapToDTOWithPartnerName(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] getDocumentContent(UUID trainerId, DocumentType documentType) {
        TrainerDocument document = documentRepository.findByTrainerIdAndDocumentType(trainerId, documentType)
                .orElseThrow(() -> new ResourceNotFoundException("Document", "trainerId and type",
                        trainerId + ", " + documentType));

        try {
            Path path = Paths.get(document.getFilePath());
            if (!Files.exists(path)) {
                throw new RuntimeException("File not found on disk");
            }
            return Files.readAllBytes(path);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read file: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TrainerDocument> getDocumentMetadata(UUID trainerId, DocumentType documentType) {
        return documentRepository.findByTrainerIdAndDocumentType(trainerId, documentType);
    }

    @Override
    public void deleteTrainer(UUID id) {
        TrainerHiring trainer = trainerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Trainer", "id", id));

        // Cleanup files
        try {
            Path trainerDir = Paths.get(uploadDir, id.toString());
            if (Files.exists(trainerDir)) {
                Files.walk(trainerDir)
                        .sorted(java.util.Comparator.reverseOrder())
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                            } catch (IOException e) {
                                log.warn("Failed to delete file: {}", path, e);
                            }
                        });
            }
        } catch (IOException e) {
            log.warn("Failed to clean up trainer files for ID: {}", id, e);
        }

        trainerRepository.delete(trainer);
    }

    // Helpers

    private TrainerHiringDTO mapToDTOWithPartnerName(TrainerHiring trainer) {
        TrainerHiringDTO dto = trainerMapper.toDTO(trainer);
        if (trainer.getPartnerId() != null) {
            com.example.mstrainerhiring.dto.PartnerHiringDTO partner = partnerClient
                    .getPartnerById(trainer.getPartnerId());
            if (partner != null) {
                dto.setPartnerName(partner.getOrganizationName());
            }
        }

        if (trainer.getJobId() != null) {
            com.example.mstrainerhiring.dto.JobDTO job = partnerClient.getJobById(trainer.getJobId());
            if (job != null) {
                dto.setJobTitle(job.getTitle());
            }
        }

        // Calculate candidate score
        dto.setScore(com.example.mstrainerhiring.utils.ScoringEngine.calculateScore(
                trainer.getYearsOfExperience(),
                trainer.getMotivationLetter(),
                trainer.getTechnology()));

        return dto;
    }

    private void validateFile(MultipartFile file, String documentName) {
        if (!ALLOWED_CONTENT_TYPE.equals(file.getContentType())) {
            throw new InvalidFileException(
                    String.format("Invalid file type for %s. Only PDF files are allowed.", documentName));
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new InvalidFileException(
                    String.format("File size for %s exceeds maximum allowed size of 5MB.", documentName));
        }
    }

    private void validateImageFile(MultipartFile file, String documentName) {
        if (!ALLOWED_IMAGE_TYPES.contains(file.getContentType())) {
            throw new InvalidFileException(
                    String.format("Invalid file type for %s. Only JPG and PNG images are allowed.", documentName));
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new InvalidFileException(
                    String.format("File size for %s exceeds maximum allowed size of 5MB.", documentName));
        }
    }

    private String storeDocument(TrainerHiring trainer, MultipartFile file, DocumentType documentType) {
        try {
            // Directory: uploads/trainers/{trainerId}/
            Path trainerDir = Paths.get(uploadDir, trainer.getId().toString());
            Files.createDirectories(trainerDir);

            String originalFilename = file.getOriginalFilename();
            String fileName = documentType.name() + "_"
                    + (originalFilename != null ? originalFilename : "document.pdf");
            Path filePath = trainerDir.resolve(fileName);

            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            TrainerDocument document = TrainerDocument.builder()
                    .trainer(trainer)
                    .documentType(documentType)
                    .fileName(fileName)
                    .filePath(filePath.toString())
                    .fileSize(file.getSize())
                    .contentType(file.getContentType())
                    .build();

            documentRepository.save(document);
            return filePath.toString();
        } catch (IOException e) {
            throw new InvalidFileException("Failed to store document: " + e.getMessage());
        }
    }

    @Override
    public TrainerHiringDTO checkApplicationExists(String email, UUID jobId) {
        return trainerRepository.findByEmailAndJobId(email, jobId)
                .map(trainerMapper::toDTO)
                .orElse(null);
    }
}
