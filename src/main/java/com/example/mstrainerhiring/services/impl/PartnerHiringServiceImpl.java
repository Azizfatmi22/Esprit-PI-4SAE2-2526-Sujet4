package com.example.mstrainerhiring.services.impl;

import com.example.mstrainerhiring.dto.PartnerHiringDTO;
import com.example.mstrainerhiring.entities.PartnerDocument;
import com.example.mstrainerhiring.entities.PartnerHiring;
import com.example.mstrainerhiring.enums.DocumentType;
import com.example.mstrainerhiring.enums.PartnerStatus;
import com.example.mstrainerhiring.exception.*;
import com.example.mstrainerhiring.mapper.PartnerHiringMapper;
import com.example.mstrainerhiring.repositories.PartnerDocumentRepository;
import com.example.mstrainerhiring.repositories.PartnerHiringRepository;
import com.example.mstrainerhiring.services.JobService;
import com.example.mstrainerhiring.services.PartnerHiringService;
import com.example.mstrainerhiring.utils.PartnerScoringEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PartnerHiringServiceImpl implements PartnerHiringService {

    private final PartnerHiringRepository partnerRepository;
    private final PartnerDocumentRepository documentRepository;
    private final PartnerHiringMapper partnerMapper;
    private final JobService jobService;

    @Value("${file.upload-dir}")
    private String uploadDir;

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final String ALLOWED_CONTENT_TYPE = "application/pdf";

    @Override
    public PartnerHiringDTO createPartner(PartnerHiringDTO partnerDTO,
            MultipartFile businessRegistration,
            MultipartFile companyProfile,
            MultipartFile logoFile) {
        log.info("Creating new partner: {}", partnerDTO.getOrganizationName());

        // Validate required documents
        if (businessRegistration == null || businessRegistration.isEmpty()) {
            throw new MissingRequiredDocumentException("BUSINESS_REGISTRATION document is required");
        }
        if (companyProfile == null || companyProfile.isEmpty()) {
            throw new MissingRequiredDocumentException("COMPANY_PROFILE document is required");
        }

        // Validate domain connection
        validateWebsiteDomain(partnerDTO.getEmail(), partnerDTO.getWebsite());

        // Validate website reachability
        validateWebsiteReachability(partnerDTO.getWebsite());

        // Check for duplicates
        if (partnerRepository.existsByEmail(partnerDTO.getEmail())) {
            throw new DuplicateResourceException("Partner", "email", partnerDTO.getEmail());
        }

        // Map DTO to entity and save to generate UUID
        PartnerHiring partner = partnerMapper.toEntity(partnerDTO);
        
        boolean isFraud = PartnerScoringEngine.isSuspicious(partner);
        partner.setStatus(isFraud ? PartnerStatus.INVESTIGATING : PartnerStatus.PENDING);
        
        PartnerScoringEngine.ScoringResult scoringResult = PartnerScoringEngine.calculateTrustScore(partner, logoFile != null && !logoFile.isEmpty());
        partner.setTrustScore(scoringResult.getScore());
        partner.setTrustAnalysis(scoringResult.getAnalysis());
        partner.setTier(PartnerScoringEngine.determineTier(scoringResult.getScore()));
        
        PartnerHiring savedPartner = partnerRepository.save(partner);
        log.info("Partner saved with ID: {} and Status: {}", savedPartner.getId(), savedPartner.getStatus());

        // Validate file type and size before storing
        validateFile(businessRegistration, "BUSINESS_REGISTRATION");
        validateFile(companyProfile, "COMPANY_PROFILE");

        // Store documents
        storeDocument(savedPartner, businessRegistration, DocumentType.BUSINESS_REGISTRATION);
        storeDocument(savedPartner, companyProfile, DocumentType.COMPANY_PROFILE);
        if (logoFile != null && !logoFile.isEmpty()) {
            storeDocument(savedPartner, logoFile, DocumentType.LOGO);
        }

        // Reload with documents
        PartnerHiring reloaded = partnerRepository.findById(savedPartner.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Partner", "id", savedPartner.getId()));

        return partnerMapper.toDTO(reloaded);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PartnerHiringDTO> getAllPartners(Pageable pageable, PartnerStatus status) {
        log.info("Fetching partners, page: {}, status: {}", pageable.getPageNumber(), status);

        Page<PartnerHiring> partnersPage;
        if (status != null) {
            partnersPage = partnerRepository.findByStatus(status, pageable);
        } else {
            partnersPage = partnerRepository.findAll(pageable);
        }

        try {
            return partnersPage.map(partnerMapper::toDTO);
        } catch (Exception e) {
            log.error("CRITICAL: Failed to map Partner entities to DTOs: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PartnerHiringDTO getPartnerById(UUID id) {
        log.info("Fetching partner with ID: {}", id);
        PartnerHiring partner = partnerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Partner", "id", id));
        return partnerMapper.toDTO(partner);
    }

    @Override
    public PartnerHiringDTO updatePartner(UUID id, PartnerHiringDTO partnerDTO) {
        log.info("Updating partner with ID: {}", id);

        PartnerHiring existingPartner = partnerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Partner", "id", id));

        // Validate domain connection
        validateWebsiteDomain(partnerDTO.getEmail(), partnerDTO.getWebsite());

        // Check for duplicates (exclude current record)
        if (partnerRepository.existsByEmailAndIdNot(partnerDTO.getEmail(), id)) {
            throw new DuplicateResourceException("Partner", "email", partnerDTO.getEmail());
        }

        // Update fields
        partnerMapper.updateEntityFromDTO(partnerDTO, existingPartner);
        
        boolean isFraud = PartnerScoringEngine.isSuspicious(existingPartner);
        if (isFraud) {
            existingPartner.setStatus(PartnerStatus.INVESTIGATING);
        }
        
        boolean hasLogo = documentRepository.findByPartnerIdAndDocumentType(id, DocumentType.LOGO).isPresent();
        PartnerScoringEngine.ScoringResult scoringResult = PartnerScoringEngine.calculateTrustScore(existingPartner, hasLogo);
        existingPartner.setTrustScore(scoringResult.getScore());
        existingPartner.setTrustAnalysis(scoringResult.getAnalysis());
        existingPartner.setTier(PartnerScoringEngine.determineTier(scoringResult.getScore()));

        PartnerHiring updatedPartner = partnerRepository.save(existingPartner);

        return partnerMapper.toDTO(updatedPartner);
    }

    @Override
    public PartnerHiringDTO updateStatus(UUID id, PartnerStatus status) {
        log.info("Updating status for partner ID {}: to {}", id, status);
        PartnerHiring partner = partnerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Partner", "id", id));
        partner.setStatus(status);
        PartnerHiring updated = partnerRepository.save(partner);
        log.info("Status updated successfully for partner: {}", id);

        return partnerMapper.toDTO(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] getDocumentContent(UUID partnerId, DocumentType documentType) {
        log.info("Getting document content for partner: {}, type: {}", partnerId, documentType);
        PartnerDocument document = documentRepository.findByPartnerIdAndDocumentType(partnerId, documentType)
                .orElseThrow(() -> {
                    log.error("Document not found in database: partner={}, type={}", partnerId, documentType);
                    return new ResourceNotFoundException("Document", "partnerId and type",
                            partnerId + ", " + documentType);
                });

        log.info("Reading file from path: {}", document.getFilePath());
        try {
            Path path = Paths.get(document.getFilePath());
            if (!Files.exists(path)) {
                log.error("File does not exist on filesystem: {}", document.getFilePath());
                throw new RuntimeException("File not found on disk");
            }
            return Files.readAllBytes(path);
        } catch (IOException e) {
            log.error("Failed to read file: {}", document.getFilePath(), e);
            throw new RuntimeException("Failed to read file: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.Optional<PartnerDocument> getDocumentMetadata(UUID partnerId, DocumentType documentType) {
        log.info("Fetching document metadata for partner: {}, type: {}", partnerId, documentType);
        return documentRepository.findByPartnerIdAndDocumentType(partnerId, documentType);
    }

    @Override
    public void deletePartner(UUID id) {
        log.info("Deleting partner with ID: {}", id);

        PartnerHiring partner = partnerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Partner", "id", id));

        // Delete all associated Jobs for this partner
        List<com.example.mstrainerhiring.entities.Job> partnerJobs = jobService.getJobsByPartner(id);
        for (com.example.mstrainerhiring.entities.Job job : partnerJobs) {
            jobService.deleteJob(job.getId());
        }

        // Delete uploaded files from filesystem
        try {
            Path partnerDir = Paths.get(uploadDir, id.toString());
            if (Files.exists(partnerDir)) {
                Files.walk(partnerDir)
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
            log.warn("Failed to clean up partner files for ID: {}", id, e);
        }

        partnerRepository.delete(partner);
        log.info("Partner deleted successfully: {}", id);
    }

    // ==================== Private Helper Methods ====================

    private void validateWebsiteReachability(String website) {
        if (website == null || website.isEmpty())
            return;

        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(website))
                    .method("HEAD", HttpRequest.BodyPublishers.noBody())
                    .timeout(Duration.ofSeconds(5))
                    .build();

            HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());

            int statusCode = response.statusCode();
            log.info("Reachability check for {}: {}", website, statusCode);

            if (statusCode < 200 || statusCode >= 400) {
                log.warn("Controle de saisie warning: Website '{}' is not reachable (HTTP {}).", website, statusCode);
                // throw new InvalidFileException(...)
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Reachability check interrupted for " + website);
        } catch (Exception e) {
            log.warn("Failed to reach website {}: {}", website, e.getMessage());
            // throw new InvalidFileException(...)
        }
    }

    private void validateWebsiteDomain(String email, String website) {
        if (email == null || website == null || website.isBlank())
            return;

        String[] parts = email.split("@");
        if (parts.length < 2)
            return;

        String domain = parts[1].toLowerCase();
        if (!website.toLowerCase().contains(domain)) {
            log.warn("Controle de saisie warning: Website '{}' should ideally contain the domain name '{}' from the business email.", website, domain);
            // throw new InvalidFileException(...)
        }
    }

    private void validateFile(MultipartFile file, String documentName) {
        if (!ALLOWED_CONTENT_TYPE.equals(file.getContentType())) {
            throw new InvalidFileException(
                    String.format("Invalid file type for %s. Only PDF files are allowed. Got: %s",
                            documentName, file.getContentType()));
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new InvalidFileException(
                    String.format("File size for %s exceeds maximum allowed size of 5MB. Got: %d bytes",
                            documentName, file.getSize()));
        }
    }

    private void storeDocument(PartnerHiring partner, MultipartFile file, DocumentType documentType) {
        try {
            // Create directory: uploads/partners/{partnerId}/
            Path partnerDir = Paths.get(uploadDir, partner.getId().toString());
            Files.createDirectories(partnerDir);

            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String fileName = documentType.name() + "_"
                    + (originalFilename != null ? originalFilename : "document.pdf");
            Path filePath = partnerDir.resolve(fileName);

            // Save file to disk
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            log.info("File stored at: {}", filePath);

            // Save document metadata to database
            PartnerDocument document = PartnerDocument.builder()
                    .partner(partner)
                    .documentType(documentType)
                    .fileName(fileName)
                    .filePath(filePath.toString())
                    .fileSize(file.getSize())
                    .contentType(file.getContentType())
                    .build();

            documentRepository.save(document);
            log.info("Document metadata saved for type: {}", documentType);

        } catch (IOException e) {
            log.error("Failed to store document: {}", documentType, e);
            throw new InvalidFileException("Failed to store document: " + documentType + ". " + e.getMessage());
        }
    }
}
