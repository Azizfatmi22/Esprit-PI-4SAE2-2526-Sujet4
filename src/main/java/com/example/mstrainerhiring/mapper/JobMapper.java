package com.example.mstrainerhiring.mapper;

import com.example.mstrainerhiring.dto.JobDTO;
import com.example.mstrainerhiring.entities.Job;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface JobMapper {

    @Mapping(target = "partnerId", source = "partner.id")
    @Mapping(target = "partnerName", source = "partner.organizationName")
    JobDTO toDTO(Job job);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "partner", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Job toEntity(JobDTO jobDTO);
}
