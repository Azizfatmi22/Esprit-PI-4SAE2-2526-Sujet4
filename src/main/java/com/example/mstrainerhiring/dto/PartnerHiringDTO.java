package com.example.mstrainerhiring.dto;

import com.example.mstrainerhiring.enums.City;
import com.example.mstrainerhiring.enums.LegalForm;
import com.example.mstrainerhiring.enums.PartnershipType;
import com.example.mstrainerhiring.enums.PartnerStatus;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PartnerHiringDTO {

    private UUID id;

    @NotBlank(message = "Organization name is required")
    @Size(min = 3, message = "Organization name must be at least 3 characters")
    private String organizationName;

    @NotNull(message = "Legal form is required")
    private LegalForm legalForm;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    @NotBlank(message = "Phone is required")
    @Pattern(regexp = "^[2579][0-9]{7}$", message = "Phone must be a valid Tunisian number")
    private String phone;

    @NotBlank(message = "Website is required")
    @Pattern(regexp = "^https://.*", message = "Website must be a valid HTTPS URL")
    private String website;

    @NotNull(message = "City is required")
    private City city;

    @NotBlank(message = "Address is required")
    @Size(min = 5, message = "Address must be at least 5 characters")
    private String address;

    @NotNull(message = "Partnership type is required")
    private PartnershipType partnershipType;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private List<PartnerDocumentDTO> documents;

    private PartnerStatus status;
}
