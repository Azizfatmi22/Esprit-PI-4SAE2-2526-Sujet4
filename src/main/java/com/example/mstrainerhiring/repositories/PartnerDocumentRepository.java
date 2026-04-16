package com.example.mstrainerhiring.repositories;

import com.example.mstrainerhiring.entities.PartnerDocument;
import com.example.mstrainerhiring.enums.DocumentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PartnerDocumentRepository extends JpaRepository<PartnerDocument, UUID> {

    List<PartnerDocument> findByPartnerId(UUID partnerId);

    java.util.Optional<PartnerDocument> findByPartnerIdAndDocumentType(UUID partnerId, DocumentType documentType);

    boolean existsByPartnerIdAndDocumentType(UUID partnerId, DocumentType documentType);

    void deleteByPartnerId(UUID partnerId);
}
