package org.example.msreportingcertification.controllers;

import lombok.RequiredArgsConstructor;
import org.example.msreportingcertification.entities.CertificateTemplate;
import org.example.msreportingcertification.entities.EvaluationHistory;
import org.example.msreportingcertification.repositories.CertificateTemplateRepository;
import org.example.msreportingcertification.repositories.EvaluationHistoryRepository;
import org.example.msreportingcertification.services.interfaces.ICertificateService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/reporting/certificates")
@RequiredArgsConstructor
public class CertificateController {

    private final ICertificateService certificateService;
    private final EvaluationHistoryRepository historyRepository;
    private final CertificateTemplateRepository templateRepository;


    @PostMapping("/template")
    public ResponseEntity<CertificateTemplate> saveTemplate(@RequestBody CertificateTemplate template) {
        System.out.println("_______________________________");
        System.out.println(template);
        return ResponseEntity.ok(certificateService.saveOrUpdateTemplate(template));
    }

    @GetMapping("/template/{evaluationId}")
    public ResponseEntity<CertificateTemplate> getTemplateByEvaluation(@PathVariable Long evaluationId) {
        return templateRepository.findByEvaluationId(evaluationId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @GetMapping("/download/{historyId}")
    public ResponseEntity<byte[]> download(@PathVariable Long historyId) throws Exception {

        EvaluationHistory history = historyRepository.findById(historyId)
                .orElseThrow(() -> new RuntimeException("Historique introuvable"));

        if ("BANNED".equalsIgnoreCase(String.valueOf(history.getVigilanceStatus()))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        CertificateTemplate template = templateRepository.findByEvaluationId(history.getEvaluationId())
                .orElseGet(() -> templateRepository.findDefaultTemplate()
                        .orElseThrow(() -> new RuntimeException("Aucun template (spécifique ou par défaut) n'a été trouvé.")));

        byte[] pdfBytes = certificateService.generateCertificatePdf(history, template);

        String fileName = "Certificate_" + history.getLearnerName().replace(" ", "_") + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName)
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }


}
