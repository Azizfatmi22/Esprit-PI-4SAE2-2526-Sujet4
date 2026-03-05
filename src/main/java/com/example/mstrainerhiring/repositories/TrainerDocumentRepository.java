package com.example.mstrainerhiring.repositories;

import com.example.mstrainerhiring.entities.TrainerDocument;
import com.example.mstrainerhiring.enums.DocumentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TrainerDocumentRepository extends JpaRepository<TrainerDocument, UUID> {
    Optional<TrainerDocument> findByTrainerIdAndDocumentType(UUID trainerId, DocumentType documentType);

    void deleteByTrainerId(UUID trainerId);
}
