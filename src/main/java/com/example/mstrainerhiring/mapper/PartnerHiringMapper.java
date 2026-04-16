package com.example.mstrainerhiring.mapper;

import com.example.mstrainerhiring.dto.PartnerDocumentDTO;
import com.example.mstrainerhiring.dto.PartnerHiringDTO;
import com.example.mstrainerhiring.entities.PartnerDocument;
import com.example.mstrainerhiring.entities.PartnerHiring;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PartnerHiringMapper {

    PartnerHiringDTO toDTO(PartnerHiring entity);

    @Mapping(target = "documents", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "trustScore", ignore = true)
    @Mapping(target = "tier", ignore = true)
    @Mapping(target = "trustAnalysis", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    PartnerHiring toEntity(PartnerHiringDTO dto);

    @Mapping(target = "documents", ignore = true)
    void updateEntityFromDTO(PartnerHiringDTO dto, @MappingTarget PartnerHiring entity);

    @Mapping(source = "partner.id", target = "partnerId")
    PartnerDocumentDTO toDocumentDTO(PartnerDocument entity);

    List<PartnerDocumentDTO> toDocumentDTOList(List<PartnerDocument> entities);

    List<PartnerHiringDTO> toDTOList(List<PartnerHiring> entities);
}
