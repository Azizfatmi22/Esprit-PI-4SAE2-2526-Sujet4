package com.example.msforum.controllers;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/images")
public class ImageController {

    @Value("${app.image.upload.dir:uploads/images}")
    private String uploadDir;

    @PostMapping("/upload")
    public ResponseEntity<ImageUploadResponse> uploadImage(@RequestParam("file") MultipartFile file,
                                                           HttpServletRequest request) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(ImageUploadResponse.builder()
                .error("File is empty")
                .build());
        }

        try {
            String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
            String contentType = file.getContentType();

            if (contentType == null || !contentType.startsWith("image/")) {
                return ResponseEntity.badRequest().body(ImageUploadResponse.builder()
                    .error("Only image files are allowed")
                    .build());
            }

            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(uploadPath);

            Path targetPath = uploadPath.resolve(fileName).normalize();
            try (var inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }

            String fileUrl = ServletUriComponentsBuilder.fromRequestUri(request)
                .replacePath("/api/images/" + fileName)
                .replaceQuery(null)
                .build()
                .toUriString();

            return ResponseEntity.ok(ImageUploadResponse.builder()
                .url(fileUrl)
                .fileName(fileName)
                .contentType(contentType)
                .size(file.getSize())
                .build());

        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(ImageUploadResponse.builder()
                .error("Failed to process image: " + e.getMessage())
                .build());
        }
    }

    @GetMapping("/{fileName:.+}")
    public ResponseEntity<Resource> getImage(@PathVariable String fileName) {
        try {
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Path filePath = uploadPath.resolve(fileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }

            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
            }

            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                .body(resource);
        } catch (MalformedURLException e) {
            return ResponseEntity.notFound().build();
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @lombok.Builder
    @lombok.Getter
    public static class ImageUploadResponse {
        private String url;
        private String fileName;
        private String contentType;
        private long size;
        private String error;
    }
}
