package com.example.mstrainerhiring.controllers;

import com.example.mstrainerhiring.dto.PartnerHiringDTO;
import com.example.mstrainerhiring.services.PartnerHiringService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.mstrainerhiring.entities.PartnerDocument;
import com.example.mstrainerhiring.enums.DocumentType;
import com.example.mstrainerhiring.enums.PartnerStatus;
import com.example.mstrainerhiring.exception.ResourceNotFoundException;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/partners")
@RequiredArgsConstructor
@Tag(name = "Partner Hiring", description = "Partner Hiring Management API")
public class PartnerHiringController {

        private final PartnerHiringService partnerService;

        @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        @Operation(summary = "Create a new partner", description = "Creates a new partner with required documents (BUSINESS_REGISTRATION and COMPANY_PROFILE)")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "201", description = "Partner created successfully"),
                        @ApiResponse(responseCode = "400", description = "Invalid input or missing documents"),
                        @ApiResponse(responseCode = "409", description = "Duplicate registration number or email")
        })
        public ResponseEntity<PartnerHiringDTO> createPartner(
                        @RequestPart("partner") @Parameter(description = "Partner data as JSON string") String partnerJson,
                        @RequestPart("businessRegistration") @Parameter(description = "Business Registration PDF (max 5MB)") MultipartFile businessRegistration,
                        @RequestPart("companyProfile") @Parameter(description = "Company Profile PDF (max 5MB)") MultipartFile companyProfile,
                        @RequestPart(value = "logo", required = false) @Parameter(description = "Partner Logo Image") MultipartFile logo)
                        throws JsonProcessingException {

                ObjectMapper objectMapper = new ObjectMapper();
                objectMapper.registerModule(new JavaTimeModule());
                PartnerHiringDTO partnerDTO = objectMapper.readValue(partnerJson, PartnerHiringDTO.class);

                PartnerHiringDTO createdPartner = partnerService.createPartner(partnerDTO, businessRegistration,
                                companyProfile, logo);
                return new ResponseEntity<>(createdPartner, HttpStatus.CREATED);
        }

        @GetMapping
        @Operation(summary = "Get all partners", description = "Returns a paginated list of all partners")
        @ApiResponse(responseCode = "200", description = "Partners retrieved successfully")
        public ResponseEntity<Page<PartnerHiringDTO>> getAllPartners(
                        @PageableDefault(size = 10, sort = "createdAt") Pageable pageable,
                        @RequestParam(name = "status", required = false) PartnerStatus status) {
                Page<PartnerHiringDTO> partners = partnerService.getAllPartners(pageable, status);
                return ResponseEntity.ok(partners);
        }

        @GetMapping("/{id}")
        @Operation(summary = "Get a partner by ID", description = "Returns a single partner by their UUID")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Partner found"),
                        @ApiResponse(responseCode = "404", description = "Partner not found")
        })
        public ResponseEntity<PartnerHiringDTO> getPartnerById(
                        @PathVariable(name = "id") @Parameter(description = "Partner UUID") UUID id) {
                PartnerHiringDTO partner = partnerService.getPartnerById(id);
                return ResponseEntity.ok(partner);
        }

        @PutMapping("/{id}")
        @Operation(summary = "Update a partner", description = "Updates partner information (no document update)")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Partner updated successfully"),
                        @ApiResponse(responseCode = "404", description = "Partner not found"),
                        @ApiResponse(responseCode = "409", description = "Duplicate registration number or email")
        })
        public ResponseEntity<PartnerHiringDTO> updatePartner(
                        @PathVariable(name = "id") @Parameter(description = "Partner UUID") UUID id,
                        @Valid @RequestBody PartnerHiringDTO partnerDTO) {
                PartnerHiringDTO updatedPartner = partnerService.updatePartner(id, partnerDTO);
                return ResponseEntity.ok(updatedPartner);
        }

        @PatchMapping("/{id}/status")
        @Operation(summary = "Update partner status", description = "Accept or Reject a partner application")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Status updated successfully"),
                        @ApiResponse(responseCode = "404", description = "Partner not found")
        })
        public ResponseEntity<PartnerHiringDTO> updateStatus(
                        @PathVariable(name = "id") @Parameter(description = "Partner UUID") UUID id,
                        @RequestParam(name = "status") @Parameter(description = "New status (ACCEPTED/REJECTED)") PartnerStatus status) {
                PartnerHiringDTO updatedPartner = partnerService.updateStatus(id, status);
                return ResponseEntity.ok(updatedPartner);
        }

        @DeleteMapping("/{id}")
        @Operation(summary = "Delete a partner", description = "Deletes a partner and all related documents")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "204", description = "Partner deleted successfully"),
                        @ApiResponse(responseCode = "404", description = "Partner not found")
        })
        public ResponseEntity<Void> deletePartner(
                        @PathVariable(name = "id") @Parameter(description = "Partner UUID") UUID id) {
                partnerService.deletePartner(id);
                return ResponseEntity.noContent().build();
        }

        @GetMapping("/{id}/documents/{type}")
        @Operation(summary = "Get partner document", description = "Returns the document content (PDF/Image)")
        public ResponseEntity<byte[]> getDocument(
                        @PathVariable(name = "id") UUID id,
                        @PathVariable(name = "type") DocumentType type) {
                byte[] content = partnerService.getDocumentContent(id, type);
                PartnerDocument metadata = partnerService
                                .getDocumentMetadata(id, type)
                                .orElseThrow(() -> new ResourceNotFoundException("Document", "type", type.name()));

                String contentType = metadata.getContentType() != null ? metadata.getContentType()
                                : "application/octet-stream";
                String disposition = "inline";

                return ResponseEntity.ok()
                                .contentType(MediaType.parseMediaType(contentType))
                                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                                                disposition + "; filename=\"" + metadata.getFileName() + "\"")
                                .body(content);
        }
}
