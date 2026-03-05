package com.example.mstrainerhiring.mapper;

import com.example.mstrainerhiring.dto.TrainerDocumentDTO;
import com.example.mstrainerhiring.dto.TrainerHiringDTO;
import com.example.mstrainerhiring.entities.TrainerDocument;
import com.example.mstrainerhiring.entities.TrainerHiring;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring")
public interface TrainerHiringMapper {

    @Mapping(target = "partnerId", source = "partnerId")
    @Mapping(target = "jobId", source = "job.id")
    @Mapping(target = "jobTitle", source = "job.title")
    @Mapping(target = "partnerName", ignore = true)
    @Mapping(target = "score", ignore = true)
    TrainerHiringDTO toDTO(TrainerHiring trainerHiring);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "documents", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "job", ignore = true)
    TrainerHiring toEntity(TrainerHiringDTO trainerHiringDTO);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "documents", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "job", ignore = true)
    void updateEntityFromDTO(TrainerHiringDTO dto, @MappingTarget TrainerHiring entity);

    @Mapping(source = "trainer.id", target = "trainerId")
    TrainerDocumentDTO toDocumentDTO(TrainerDocument entity);

    List<TrainerDocumentDTO> toDocumentDTOList(List<TrainerDocument> entities);

    List<TrainerHiringDTO> toDTOList(List<TrainerHiring> entities);
}
