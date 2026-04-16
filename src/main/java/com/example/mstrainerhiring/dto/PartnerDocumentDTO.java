package com.example.mstrainerhiring.dto;

import com.example.mstrainerhiring.enums.DocumentType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PartnerDocumentDTO {

    private UUID id;
    private UUID partnerId;
    private DocumentType documentType;
    private String fileName;
    private String filePath;
    private Long fileSize;
    private String contentType;
    private LocalDateTime uploadedAt;
}
