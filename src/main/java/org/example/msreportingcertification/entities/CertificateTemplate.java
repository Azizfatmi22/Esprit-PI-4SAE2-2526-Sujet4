package org.example.msreportingcertification.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "certificate_templates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CertificateTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private Long evaluationId;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String htmlContent;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String platformLogoBase64;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String trainerSignatureBase64;

    private String trainerName;

    @com.fasterxml.jackson.annotation.JsonProperty("isTemplateDefault")
    private boolean isTemplateDefault = false;
}