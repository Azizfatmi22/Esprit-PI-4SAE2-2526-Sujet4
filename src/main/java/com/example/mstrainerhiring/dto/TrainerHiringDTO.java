package com.example.mstrainerhiring.dto;

import com.example.mstrainerhiring.enums.City;
import com.example.mstrainerhiring.enums.Technology;
import com.example.mstrainerhiring.enums.TrainerStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrainerHiringDTO {

    private UUID id;

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Forename is required")
    private String forename;

    @NotNull(message = "Location is required")
    private City location;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    @NotBlank(message = "Phone is required")
    @Pattern(regexp = "^[0-9+ \\-()]{7,20}$", message = "Phone must be a valid contact number")
    private String phone;

    @NotBlank(message = "Motivation letter is required")
    private String motivationLetter;

    @NotNull(message = "Partner selection is required")
    private UUID partnerId;

    private UUID jobId;
    private String jobTitle;

    // Intelligence Scores
    private Double skillSyncScore;
    private Boolean plagiarismFlag;
    private Double toneClarityScore;
    private Double acceptanceProbability;
    private String intelligentAnalysisContext;
    private Boolean isBlankCv;

    private Integer yearsOfExperience;

    private Technology technology;

    // Read-only field for display
    private String partnerName;

    private TrainerStatus status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private List<TrainerDocumentDTO> documents;

    private Integer score;
}
