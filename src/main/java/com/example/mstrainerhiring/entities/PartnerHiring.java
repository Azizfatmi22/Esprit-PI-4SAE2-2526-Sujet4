package com.example.mstrainerhiring.entities;

import com.example.mstrainerhiring.enums.City;
import com.example.mstrainerhiring.enums.LegalForm;
import com.example.mstrainerhiring.enums.PartnershipType;
import com.example.mstrainerhiring.enums.PartnerStatus;
import com.example.mstrainerhiring.enums.PartnerTier;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "partner_hiring")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PartnerHiring {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @NotBlank(message = "Organization name is required")
    @Size(min = 3, message = "Organization name must be at least 3 characters")
    @Column(name = "organization_name", nullable = false)
    private String organizationName;

    @NotNull(message = "Legal form is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "legal_form", nullable = false)
    private LegalForm legalForm;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @NotBlank(message = "Phone is required")
    @Pattern(regexp = "^[0-9+ \\-()]{7,20}$", message = "Phone must be a valid contact number (7-20 digits)")
    @Column(name = "phone", nullable = false)
    private String phone;

    @Column(name = "website")
    private String website;

    @NotNull(message = "City is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "city", nullable = false)
    private City city;

    @NotBlank(message = "Address is required")
    @Size(min = 5, message = "Address must be at least 5 characters")
    @Column(name = "address", nullable = false)
    private String address;

    @NotNull(message = "Partnership type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "partnership_type", nullable = false)
    private PartnershipType partnershipType;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private PartnerStatus status = PartnerStatus.PENDING;

    @Column(name = "trust_score")
    @Builder.Default
    private Integer trustScore = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "tier")
    @Builder.Default
    private PartnerTier tier = PartnerTier.BRONZE;

    @Column(name = "trust_analysis", length = 5000)
    private String trustAnalysis;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "partner", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<PartnerDocument> documents = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
