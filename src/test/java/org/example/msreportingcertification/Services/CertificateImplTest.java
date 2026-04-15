package org.example.msreportingcertification.Services;


import org.example.msreportingcertification.entities.CertificateTemplate;
import org.example.msreportingcertification.entities.EvaluationHistory;
import org.example.msreportingcertification.repositories.CertificateTemplateRepository;
import org.example.msreportingcertification.services.impl.CertificateImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CertificateImplTest {

    @Mock
    private CertificateTemplateRepository templateRepository;

    @InjectMocks
    private CertificateImpl certificateService;

    private CertificateTemplate template;
    private EvaluationHistory history;

    @BeforeEach
    void setUp() {
        template = new CertificateTemplate();
        template.setId(1L);
        template.setEvaluationId(10L);
        template.setHtmlContent("<html><body><h1>[[LEARNER_NAME]]</h1><p>[[EVALUATION_TITLE]]</p></body></html>");
        template.setTrainerName("Coach Java");

        history = new EvaluationHistory();
        history.setId(500L);
        history.setLearnerName("Omar Aguil");
        history.setEvaluationTitle("FullStack Certification");
        history.setPercentage(92.5);
        history.setReceivedAt(LocalDateTime.now());
    }

    @Test
    void saveOrUpdateTemplate_ShouldUpdateExisting_WhenEvaluationIdExists() {
        // Arrange
        when(templateRepository.findByEvaluationId(10L)).thenReturn(Optional.of(template));
        when(templateRepository.save(any(CertificateTemplate.class))).thenAnswer(i -> i.getArguments()[0]);

        CertificateTemplate updatedData = new CertificateTemplate();
        updatedData.setEvaluationId(10L);
        updatedData.setTrainerName("New Trainer");

        // Act
        CertificateTemplate result = certificateService.saveOrUpdateTemplate(updatedData);

        // Assert
        assertThat(result.getTrainerName()).isEqualTo("New Trainer");
        verify(templateRepository).save(template);
    }

    @Test
    void generateCertificatePdf_ShouldReturnByteArray_AndNotFail() throws Exception {
        // Ce test vérifie que la chaîne de génération (QR Code + PDF) ne lance pas d'exception
        // Act
        byte[] pdfBytes = certificateService.generateCertificatePdf(history, template);

        // Assert
        assertThat(pdfBytes).isNotNull();
        assertThat(pdfBytes.length).isGreaterThan(0);
    }

    @Test
    void saveOrUpdateTemplate_ShouldCreateDefault_WhenNoEvaluationId() {
        // Arrange
        when(templateRepository.findDefaultTemplate()).thenReturn(Optional.empty());
        when(templateRepository.save(any(CertificateTemplate.class))).thenAnswer(i -> i.getArguments()[0]);

        CertificateTemplate newDefault = new CertificateTemplate();
        newDefault.setEvaluationId(null);
        newDefault.setHtmlContent("Default Content");

        // Act
        CertificateTemplate result = certificateService.saveOrUpdateTemplate(newDefault);

        // Assert
        assertThat(result.isTemplateDefault()).isTrue();
        verify(templateRepository).save(any(CertificateTemplate.class));
    }
}