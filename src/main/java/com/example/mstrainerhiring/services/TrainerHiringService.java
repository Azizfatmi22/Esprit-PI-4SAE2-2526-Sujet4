package com.example.mstrainerhiring.services;

import com.example.mstrainerhiring.dto.TrainerHiringDTO;
import com.example.mstrainerhiring.entities.TrainerDocument;
import com.example.mstrainerhiring.enums.DocumentType;
import com.example.mstrainerhiring.enums.Technology;
import com.example.mstrainerhiring.enums.TrainerStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;
import java.util.UUID;

public interface TrainerHiringService {

    TrainerHiringDTO createTrainer(TrainerHiringDTO trainerDTO, MultipartFile cvFile, MultipartFile pictureFile);

    Page<TrainerHiringDTO> getAllTrainers(Pageable pageable, TrainerStatus status, String keyword,
            Technology technology);

    TrainerHiringDTO getTrainerById(UUID id);

    com.example.mstrainerhiring.entities.TrainerHiring getTrainerEntityById(UUID id);

    TrainerHiringDTO updateStatus(UUID id, TrainerStatus status);

    TrainerHiringDTO checkApplicationExists(String email, UUID jobId);

    byte[] getDocumentContent(UUID trainerId, DocumentType documentType);

    Optional<TrainerDocument> getDocumentMetadata(UUID trainerId, DocumentType documentType);

    void deleteTrainer(UUID id);
}
