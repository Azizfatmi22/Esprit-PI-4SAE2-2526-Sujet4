package com.example.mstrainerhiring.dto;

import com.example.mstrainerhiring.enums.City;
import com.example.mstrainerhiring.enums.Technology;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobDTO {
    private UUID id;
    private String title;
    private String description;
    private Technology technology;
    private City location;
    private Integer minExperience;
    private Integer maxExperience;
    private String salaryRange;
    private UUID partnerId;
    private String partnerName;
    private LocalDateTime createdAt;
}
