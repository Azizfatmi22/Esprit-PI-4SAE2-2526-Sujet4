package com.example.mstrainerhiring.services.impl;

import com.example.mstrainerhiring.entities.TrainerHiring;
import com.example.mstrainerhiring.services.ContractGenerationService;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContractGenerationServiceImpl implements ContractGenerationService {

    private final TemplateEngine templateEngine;

    @Override
    public byte[] generateContractPdf(TrainerHiring trainer) throws Exception {
        log.info("Generating Elegant PDF Contract for Trainer: {}", trainer.getId());

        // Prepare variables for the Thymeleaf HTML template
        Context context = new Context();
        context.setVariable("trainerName", trainer.getName() + " " + trainer.getForename());
        context.setVariable("trainerEmail", trainer.getEmail());
        context.setVariable("trainerPhone", trainer.getPhone() != null ? trainer.getPhone() : "N/A");
        context.setVariable("technology", trainer.getTechnology());
        context.setVariable("issueDate", LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")));

        // Mock partner info since Partner entity belongs to another MS, but we know
        // it's a partner platform contract.
        context.setVariable("partnerName", "E-Learning Premium Partner");

        // Render HTML using Thymeleaf
        String htmlContent = templateEngine.process("contract-template", context);

        // Convert HTML to PDF byte array using OpenHTMLToPDF
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            // builder.useFastMode(); // Removed for stability in v1.0.10
            
            // Explicitly use UTF-8 and ensure baseUri is handled safely
            builder.withHtmlContent(htmlContent, "."); 
            builder.toStream(outputStream);
            builder.run();

            log.info("PDF Contract successfully serialized to stream (Size: {} bytes)", outputStream.size());
            return outputStream.toByteArray();
        } catch (Exception e) {
            log.error("CRITICAL: Failed to render PDF contract for trainer {}: {}. Stacktrace follows.", trainer.getId(), e.getMessage(), e);
            throw new RuntimeException("PDF Rendering failed: " + e.getMessage(), e);
        }
    }
}
