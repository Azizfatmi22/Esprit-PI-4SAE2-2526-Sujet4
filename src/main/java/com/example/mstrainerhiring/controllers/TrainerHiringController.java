package com.example.mstrainerhiring.controllers;

import com.example.mstrainerhiring.dto.TrainerHiringDTO;
import com.example.mstrainerhiring.entities.TrainerDocument;
import com.example.mstrainerhiring.enums.DocumentType;
import com.example.mstrainerhiring.enums.Technology;
import com.example.mstrainerhiring.enums.TrainerStatus;
import com.example.mstrainerhiring.exception.ResourceNotFoundException;
import com.example.mstrainerhiring.services.TrainerHiringService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/trainers")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Trainer Hiring", description = "Trainer Hiring Management API")
public class TrainerHiringController {

        private final TrainerHiringService trainerService;
        private final ObjectMapper objectMapper;
        private final com.example.mstrainerhiring.services.RecommendationService recommendationService;

        @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        @Operation(summary = "Create a new trainer", description = "Creates a new trainer application with CV")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "201", description = "Trainer created successfully"),
                        @ApiResponse(responseCode = "400", description = "Invalid input or missing CV")
        })
        public ResponseEntity<TrainerHiringDTO> createTrainer(
                        @RequestPart("trainer") @Parameter(description = "Trainer data as JSON string") String trainerJson,
                        @RequestPart("cv") @Parameter(description = "CV PDF (max 5MB)") MultipartFile cv,
                        @RequestPart(value = "picture", required = false) @Parameter(description = "Profile Picture (JPG/PNG, max 5MB)") MultipartFile picture)
                        throws JsonProcessingException {

                TrainerHiringDTO trainerDTO = objectMapper.readValue(trainerJson, TrainerHiringDTO.class);

                TrainerHiringDTO createdTrainer = trainerService.createTrainer(trainerDTO, cv, picture);
                return new ResponseEntity<>(createdTrainer, HttpStatus.CREATED);
        }

        @GetMapping
        @Operation(summary = "Get all trainers", description = "Returns a paginated list of all trainers")
        public ResponseEntity<Page<TrainerHiringDTO>> getAllTrainers(
                        @PageableDefault(size = 10, sort = "createdAt") Pageable pageable,
                        @RequestParam(name = "status", required = false) TrainerStatus status,
                        @RequestParam(name = "keyword", required = false) String keyword,
                        @RequestParam(name = "technology", required = false) Technology technology) {
                Page<TrainerHiringDTO> trainers = trainerService.getAllTrainers(pageable, status, keyword, technology);
                return ResponseEntity.ok(trainers);
        }

        @GetMapping("/{id}")
        @Operation(summary = "Get a trainer by ID")
        public ResponseEntity<TrainerHiringDTO> getTrainerById(@PathVariable(name = "id") UUID id) {
                TrainerHiringDTO trainer = trainerService.getTrainerById(id);
                return ResponseEntity.ok(trainer);
        }

        @PatchMapping("/{id}/status")
        @Operation(summary = "Update trainer status (Approve/Reject)")
        public ResponseEntity<TrainerHiringDTO> updateStatus(
                        @PathVariable(name = "id") UUID id,
                        @RequestParam(name = "status") TrainerStatus status) {
                TrainerHiringDTO updatedTrainer = trainerService.updateStatus(id, status);
                return ResponseEntity.ok(updatedTrainer);
        }

        @GetMapping("/{id}/documents/{type}")
        @Operation(summary = "Get trainer document (CV)", description = "Returns the document content")
        public ResponseEntity<byte[]> getDocument(
                        @PathVariable(name = "id") UUID id,
                        @PathVariable(name = "type") DocumentType type) {

                byte[] content = trainerService.getDocumentContent(id, type);
                TrainerDocument metadata = trainerService.getDocumentMetadata(id, type)
                                .orElseThrow(() -> new ResourceNotFoundException("Document", "type", type.name()));

                String contentType = metadata.getContentType() != null ? metadata.getContentType()
                                : "application/octet-stream";

                return ResponseEntity.ok()
                                .contentType(MediaType.parseMediaType(contentType))
                                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                                                "inline; filename=\"" + metadata.getFileName() + "\"")
                                .body(content);
        }

        @GetMapping("/check")
        @Operation(summary = "Check if application exists for email and job")
        public ResponseEntity<TrainerHiringDTO> checkApplication(
                        @RequestParam(name = "email") String email,
                        @RequestParam(name = "jobId") UUID jobId) {
                TrainerHiringDTO trainer = trainerService.checkApplicationExists(email, jobId);
                if (trainer != null) {
                        return ResponseEntity.ok(trainer);
                }
                // Return 204 No Content instead of 404 to avoid browser console errors for
                // normal checks
                return ResponseEntity.noContent().build();
        }

        private final com.example.mstrainerhiring.services.ContractGenerationService contractGenerationService;

        @GetMapping("/{id}/contract/download")
        @Operation(summary = "Download Trainer Contract", description = "Generates and returns an elegant PDF contract for an accepted trainer")
        public ResponseEntity<byte[]> downloadContract(@PathVariable(name = "id") UUID id) {
                try {
                        com.example.mstrainerhiring.entities.TrainerHiring trainer = trainerService
                                        .getTrainerEntityById(id);
                        if (trainer.getStatus() != TrainerStatus.ACCEPTED) {
                                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
                        }

                        byte[] pdfContent = contractGenerationService.generateContractPdf(trainer);

                        return ResponseEntity.ok()
                                        .contentType(MediaType.APPLICATION_PDF)
                                        .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                                                        "attachment; filename=\"Contract_" + trainer.getName()
                                                                        + ".pdf\"")
                                        .body(pdfContent);
                } catch (Exception e) {
                        log.error("Failed to generate/download contract for trainer {}: {}", id, e.getMessage(), e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
                }
        }

        @GetMapping("/jobs/{jobId}/top-candidate")
        @Operation(summary = "Get best matched candidate", description = "Analyzes all pending applications to find the top match")
        public ResponseEntity<com.example.mstrainerhiring.dto.TopCandidateDTO> getTopCandidate(
                        @PathVariable(name = "jobId") UUID jobId) {
                com.example.mstrainerhiring.dto.TopCandidateDTO topCandidate = recommendationService
                                .getTopCandidateForJob(jobId);
                return ResponseEntity.ok(topCandidate);
        }

        @DeleteMapping("/{id}")
        @Operation(summary = "Delete a trainer application")
        public ResponseEntity<Void> deleteTrainer(@PathVariable UUID id) {
                trainerService.deleteTrainer(id);
                return ResponseEntity.noContent().build();
        }
}
