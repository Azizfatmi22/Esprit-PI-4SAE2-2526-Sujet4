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
    PartnerHiring toEntity(PartnerHiringDTO dto);

    @Mapping(target = "documents", ignore = true)
    void updateEntityFromDTO(PartnerHiringDTO dto, @MappingTarget PartnerHiring entity);

    @Mapping(source = "partner.id", target = "partnerId")
    PartnerDocumentDTO toDocumentDTO(PartnerDocument entity);

    List<PartnerDocumentDTO> toDocumentDTOList(List<PartnerDocument> entities);

    List<PartnerHiringDTO> toDTOList(List<PartnerHiring> entities);
}
