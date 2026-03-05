package com.example.mstrainerhiring.services;

import com.example.mstrainerhiring.dto.PartnerHiringDTO;
import com.example.mstrainerhiring.entities.PartnerDocument;
import com.example.mstrainerhiring.enums.DocumentType;
import com.example.mstrainerhiring.enums.PartnerStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface PartnerHiringService {

    PartnerHiringDTO createPartner(PartnerHiringDTO partnerDTO,
            MultipartFile businessRegistration,
            MultipartFile companyProfile,
            MultipartFile logo);

    Page<PartnerHiringDTO> getAllPartners(Pageable pageable, PartnerStatus status);

    PartnerHiringDTO getPartnerById(UUID id);

    PartnerHiringDTO updatePartner(UUID id, PartnerHiringDTO partnerDTO);

    PartnerHiringDTO updateStatus(UUID id, PartnerStatus status);

    byte[] getDocumentContent(UUID partnerId, DocumentType documentType);

    java.util.Optional<PartnerDocument> getDocumentMetadata(UUID partnerId, DocumentType documentType);

    void deletePartner(UUID id);
}
