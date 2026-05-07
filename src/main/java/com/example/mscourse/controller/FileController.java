package com.example.mscourse.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/courses/uploads")
public class FileController {

    private static final Logger logger = LoggerFactory.getLogger(FileController.class);

    // ── Constants ─────────────────────────────────────────────────────────────
    private static final String SUBFOLDER_IMAGES  = "images";
    private static final String SUBFOLDER_VIDEOS  = "videos";
    private static final String SUBFOLDER_PDFS    = "pdfs";
    private static final String SUBFOLDER_FILES   = "files";
    private static final String SUBFOLDER_CONTENT = "content";

    private static final String CONTENT_TYPE_OCTET  = "application/octet-stream";
    private static final String CONTENT_TYPE_JPEG   = "image/jpeg";
    private static final String CONTENT_TYPE_PNG    = "image/png";
    private static final String CONTENT_TYPE_GIF    = "image/gif";
    private static final String CONTENT_TYPE_MP4    = "video/mp4";
    private static final String CONTENT_TYPE_PDF    = "application/pdf";

    // ── Map key constants ─────────────────────────────────────────────────────
    private static final String KEY_FILE_URL        = "fileUrl";
    private static final String KEY_FILE_NAME       = "fileName";
    private static final String KEY_FILE_SIZE       = "fileSize";
    private static final String KEY_COURS_ID        = "coursId";
    private static final String KEY_CHAPITRE_ID     = "chapitreId";
    private static final String KEY_CONTENT_BLOCK_ID = "contentBlockId";
    private static final String KEY_TYPE            = "type";
    private static final String KEY_MESSAGE         = "message";
    private static final String KEY_ERROR           = "error";
    private static final String KEY_TYPE_FOLDER     = "typeFolder";
    private static final String KEY_RELATIVE_PATH   = "relativePath";
    private static final String KEY_FULL_PATH       = "fullPath";
    private static final String KEY_FILE_EXISTS     = "fileExists";

    @Value("${file.upload.dir:./uploads}")
    private String uploadDir;

    @PostConstruct
    public void init() {
        logger.info("Upload directory configured: {}", uploadDir);
        try {
            Path rootPath = Paths.get(uploadDir);
            if (!Files.exists(rootPath)) {
                Files.createDirectories(rootPath);
                logger.info("Created root upload directory: {}", rootPath);
            }
        } catch (IOException e) {
            logger.error("Could not initialize upload directory", e);
        }
    }

    // ==================== DEBUG METHODS ====================

    /**
     * Debug endpoint to check upload directory configuration.
     */
    @GetMapping("/debug/config")
    public ResponseEntity<Map<String, Object>> getConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("uploadDir", uploadDir);
        config.put("absolutePath", Paths.get(uploadDir).toAbsolutePath().toString());
        config.put("exists", Files.exists(Paths.get(uploadDir)));
        return ResponseEntity.ok(config);
    }

    /**
     * Debug endpoint to test path matching.
     */
    @GetMapping("/debug/test/{coursId}/{chapitreId}/{contentBlockId}/{type}/{filename:.+}")
    public ResponseEntity<Map<String, Object>> testPathMatching(
            @PathVariable Long coursId,
            @PathVariable Long chapitreId,
            @PathVariable Long contentBlockId,
            @PathVariable String type,
            @PathVariable String filename) {

        String typeFolder = determineSubfolder(type);
        String relativePath = String.format("cours_%d/chapitre_%d/content_block_%d/%s/%s",
                coursId, chapitreId, contentBlockId, typeFolder, filename);
        Path fullPath = Paths.get(uploadDir, relativePath);

        Map<String, Object> result = new HashMap<>();
        result.put(KEY_COURS_ID, coursId);
        result.put(KEY_CHAPITRE_ID, chapitreId);
        result.put(KEY_CONTENT_BLOCK_ID, contentBlockId);
        result.put(KEY_TYPE, type);
        result.put(KEY_FILE_NAME, filename);
        result.put(KEY_TYPE_FOLDER, typeFolder);
        result.put(KEY_RELATIVE_PATH, relativePath);
        result.put(KEY_FULL_PATH, fullPath.toAbsolutePath().toString());
        result.put(KEY_FILE_EXISTS, Files.exists(fullPath));

        return ResponseEntity.ok(result);
    }

    // ==================== UPLOAD METHODS ====================

    /**
     * Upload content file with course and chapter structure.
     * Files are named by content block ID: {id}.ext (e.g., 19.jpg)
     */
    @PostMapping("/content")
    public ResponseEntity<Map<String, Object>> uploadContentFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("type") String type,
            @RequestParam("coursId") Long coursId,
            @RequestParam("chapitreId") Long chapitreId,
            @RequestParam("contentBlockId") Long contentBlockId) {

        Map<String, Object> response = new HashMap<>();

        if (file.isEmpty()) {
            response.put(KEY_ERROR, "Le fichier est vide");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            String originalFilename = file.getOriginalFilename();
            String extension = extractExtension(originalFilename);
            String typeFolder = determineSubfolder(type);
            String filename = contentBlockId + extension;

            String uploadPath = String.format("%s/cours_%d/chapitre_%d/content_block_%d/%s/",
                    uploadDir, coursId, chapitreId, contentBlockId, typeFolder);
            String fileUrl = String.format("/api/courses/uploads/cours_%d/chapitre_%d/content_block_%d/%s/%s",
                    coursId, chapitreId, contentBlockId, typeFolder, filename);

            saveFile(uploadPath, filename, file.getBytes());

            response.put(KEY_FILE_URL, fileUrl);
            response.put(KEY_FILE_NAME, originalFilename);
            response.put(KEY_FILE_SIZE, file.getSize());
            response.put("contentType", file.getContentType());
            response.put(KEY_COURS_ID, coursId);
            response.put(KEY_CHAPITRE_ID, chapitreId);
            response.put(KEY_CONTENT_BLOCK_ID, contentBlockId);
            response.put(KEY_TYPE, typeFolder);
            response.put(KEY_MESSAGE, "Fichier uploadé avec succès");

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            logger.error("IO Error during file upload", e);
            response.put(KEY_ERROR, "Erreur lors de la sauvegarde: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        } catch (Exception e) {
            logger.error("Unexpected error during file upload", e);
            response.put(KEY_ERROR, "Erreur inattendue: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Upload multiple files.
     */
    @PostMapping("/content/bulk")
    public ResponseEntity<Map<String, Object>> uploadMultipleFiles(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam("type") String type,
            @RequestParam("coursId") Long coursId,
            @RequestParam("chapitreId") Long chapitreId) {

        Map<String, Object> response = new HashMap<>();
        List<Map<String, String>> uploadedFiles = new ArrayList<>();

        try {
            for (MultipartFile file : files) {
                if (!file.isEmpty()) {
                    uploadedFiles.add(processBulkFile(file, type, coursId, chapitreId));
                }
            }

            response.put(SUBFOLDER_FILES, uploadedFiles);
            response.put("count", uploadedFiles.size());
            response.put(KEY_TYPE, type);
            response.put(KEY_COURS_ID, coursId);
            response.put(KEY_CHAPITRE_ID, chapitreId);

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            logger.error("IO Error during bulk upload", e);
            response.put(KEY_ERROR, "Erreur lors de l'upload multiple: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Upload thumbnail. Filename: {coursId}.ext
     */
    @PostMapping("/thumbnails/upload")
    public ResponseEntity<Map<String, Object>> uploadThumbnail(
            @RequestParam("file") MultipartFile file,
            @RequestParam("coursId") Long coursId) {

        Map<String, Object> response = new HashMap<>();

        if (file.isEmpty()) {
            response.put(KEY_ERROR, "Le fichier est vide");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            String originalFilename = file.getOriginalFilename();
            String extension = extractExtension(originalFilename);
            String filename = coursId + extension;
            String uploadPath = String.format("%s/cours_%d/thumbnails/", uploadDir, coursId);

            saveFile(uploadPath, filename, file.getBytes());

            String fileUrl = String.format("/api/courses/uploads/cours_%d/thumbnails/%s", coursId, filename);

            response.put(KEY_FILE_URL, fileUrl);
            response.put(KEY_FILE_NAME, originalFilename);
            response.put(KEY_FILE_SIZE, file.getSize());
            response.put(KEY_COURS_ID, coursId);
            response.put(KEY_MESSAGE, "Thumbnail uploadé avec succès");

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            logger.error("IO Error during thumbnail upload", e);
            response.put(KEY_ERROR, "Erreur lors de l'upload de la miniature: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ==================== SERVE METHODS ====================

    @GetMapping("/cours_{coursId}/chapitre_{chapitreId}/content_block_{contentBlockId}/{type}/{filename:.+}")
    public ResponseEntity<Resource> serveCourseFileWithContentBlock(
            @PathVariable Long coursId,
            @PathVariable Long chapitreId,
            @PathVariable Long contentBlockId,
            @PathVariable String type,
            @PathVariable String filename) {

        String typeFolder = determineSubfolder(type);
        String filePath = String.format("cours_%d/chapitre_%d/content_block_%d/%s/%s",
                coursId, chapitreId, contentBlockId, typeFolder, filename);
        return serveFile(filePath, filename);
    }

    @GetMapping("/cours_{coursId}/chapitre_{chapitreId}/{type}/{filename:.+}")
    public ResponseEntity<Resource> serveCourseFile(
            @PathVariable Long coursId,
            @PathVariable Long chapitreId,
            @PathVariable String type,
            @PathVariable String filename) {

        String typeFolder = determineSubfolder(type);
        String filePath = String.format("cours_%d/chapitre_%d/%s/%s",
                coursId, chapitreId, typeFolder, filename);
        return serveFile(filePath, filename);
    }

    @GetMapping("/cours_{coursId}/chapitre_{chapitreId}/temp/{type}/{filename:.+}")
    public ResponseEntity<Resource> serveTempFile(
            @PathVariable Long coursId,
            @PathVariable Long chapitreId,
            @PathVariable String type,
            @PathVariable String filename) {

        String typeFolder = determineSubfolder(type);
        String filePath = String.format("cours_%d/chapitre_%d/temp/%s/%s",
                coursId, chapitreId, typeFolder, filename);
        return serveFile(filePath, filename);
    }

    @GetMapping("/{coursId}/thumbnails/{filename:.+}")
    public ResponseEntity<Resource> serveThumbnail(
            @PathVariable Long coursId,
            @PathVariable String filename) {

        return serveFile(String.format("cours_%d/thumbnails/%s", coursId, filename), filename);
    }

    @GetMapping("/cours_{coursId}/attachments/{filename:.+}")
    public ResponseEntity<Resource> serveAttachment(
            @PathVariable Long coursId,
            @PathVariable String filename) {

        return serveFile(String.format("cours_%d/attachments/%s", coursId, filename), filename);
    }

    @GetMapping("/images/{filename:.+}")
    public ResponseEntity<Resource> serveImage(@PathVariable String filename) {
        return serveFile("images/" + filename, filename);
    }

    @GetMapping("/videos/{filename:.+}")
    public ResponseEntity<Resource> serveVideo(@PathVariable String filename) {
        return serveFile("videos/" + filename, filename);
    }

    @GetMapping("/pdfs/{filename:.+}")
    public ResponseEntity<Resource> servePdf(@PathVariable String filename) {
        return serveFile("pdfs/" + filename, filename);
    }

    @GetMapping("/content/{filename:.+}")
    public ResponseEntity<Resource> serveContent(@PathVariable String filename) {
        return serveFile("content/" + filename, filename);
    }

    // ==================== UTILITY ENDPOINTS ====================

    @DeleteMapping("/cours_{coursId}/chapitre_{chapitreId}/content_block_{contentBlockId}/{type}/{filename:.+}")
    public ResponseEntity<Map<String, String>> deleteFile(
            @PathVariable Long coursId,
            @PathVariable Long chapitreId,
            @PathVariable Long contentBlockId,
            @PathVariable String type,
            @PathVariable String filename) {

        try {
            String typeFolder = determineSubfolder(type);
            String filePath = String.format("%s/cours_%d/chapitre_%d/content_block_%d/%s/%s",
                    uploadDir, coursId, chapitreId, contentBlockId, typeFolder, filename);

            Path path = Paths.get(filePath);
            if (Files.exists(path)) {
                Files.delete(path);
                logger.info("File deleted: {}", path);
                return ResponseEntity.ok(Map.of(KEY_MESSAGE, "Fichier supprimé avec succès"));
            }
            return ResponseEntity.notFound().build();

        } catch (IOException e) {
            logger.error("Error deleting file", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(KEY_ERROR, "Erreur lors de la suppression: " + e.getMessage()));
        }
    }

    @GetMapping("/cours_{coursId}/chapitre_{chapitreId}/content_block_{contentBlockId}/{type}/list")
    public ResponseEntity<List<Map<String, String>>> listFiles(
            @PathVariable Long coursId,
            @PathVariable Long chapitreId,
            @PathVariable Long contentBlockId,
            @PathVariable String type) {

        try {
            String typeFolder = determineSubfolder(type);
            String dirPath = String.format("%s/cours_%d/chapitre_%d/content_block_%d/%s/",
                    uploadDir, coursId, chapitreId, contentBlockId, typeFolder);

            Path path = Paths.get(dirPath);
            if (!Files.exists(path)) {
                return ResponseEntity.ok(List.of());
            }

            List<Map<String, String>> files = new ArrayList<>();
            try (java.util.stream.Stream<Path> stream = Files.list(path)) {
                stream.forEach(filePath -> {
                    String fname = filePath.getFileName().toString();
                    Map<String, String> fileInfo = new HashMap<>();
                    fileInfo.put(KEY_FILE_NAME, fname);
                    fileInfo.put(KEY_FILE_URL, String.format(
                            "/api/courses/uploads/cours_%d/chapitre_%d/content_block_%d/%s/%s",
                            coursId, chapitreId, contentBlockId, typeFolder, fname));
                    try {
                        fileInfo.put(KEY_FILE_SIZE, String.valueOf(Files.size(filePath)));
                    } catch (IOException e) {
                        fileInfo.put(KEY_FILE_SIZE, "0");
                    }
                    files.add(fileInfo);
                });
            }

            return ResponseEntity.ok(files);

        } catch (IOException e) {
            logger.error("Error listing files", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ==================== PRIVATE HELPERS ====================

    private Map<String, String> processBulkFile(MultipartFile file, String type,
                                                  Long coursId, Long chapitreId) throws IOException {
        String originalFilename = file.getOriginalFilename();
        String extension = extractExtension(originalFilename);
        String nameWithoutExt = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(0, originalFilename.lastIndexOf("."))
                : originalFilename;
        String safeFilename = nameWithoutExt != null
                ? nameWithoutExt.replaceAll("[^a-zA-Z0-9.-]", "_") : "file";
        String filename = System.currentTimeMillis() + "_" + safeFilename + extension;

        String typeFolder = determineSubfolder(type);
        String uploadPath = String.format("%s/cours_%d/chapitre_%d/%s/",
                uploadDir, coursId, chapitreId, typeFolder);

        saveFile(uploadPath, filename, file.getBytes());

        Map<String, String> fileInfo = new HashMap<>();
        fileInfo.put(KEY_FILE_URL, String.format("/api/courses/uploads/cours_%d/chapitre_%d/%s/%s",
                coursId, chapitreId, typeFolder, filename));
        fileInfo.put(KEY_FILE_NAME, originalFilename != null ? originalFilename : filename);
        fileInfo.put(KEY_FILE_SIZE, String.valueOf(file.getSize()));
        return fileInfo;
    }

    private void saveFile(String dirPath, String filename, byte[] bytes) throws IOException {
        Path dir = Paths.get(dirPath);
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
            logger.info("Created directory: {}", dir);
        }
        Path filePath = dir.resolve(filename);
        Files.write(filePath, bytes);
        logger.info("File saved: {}", filePath);
    }

    private ResponseEntity<Resource> serveFile(String relativePath, String filename) {
        try {
            Path filePath = Paths.get(uploadDir, relativePath);
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                String contentType = determineContentType(filename);
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CONTENT_DISPOSITION,
                                "inline; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            }
            logger.warn("File not found or not readable: {}", filePath);
            return ResponseEntity.notFound().build();

        } catch (Exception e) {
            logger.error("Error serving file: {}", relativePath, e);
            return ResponseEntity.notFound().build();
        }
    }

    private String extractExtension(String originalFilename) {
        if (originalFilename != null && originalFilename.contains(".")) {
            return originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        return "";
    }

    private String determineSubfolder(String type) {
        if (type == null) return SUBFOLDER_CONTENT;
        return switch (type.toUpperCase()) {
            case "IMAGE", "IMAGES" -> SUBFOLDER_IMAGES;
            case "VIDEO", "VIDEOS" -> SUBFOLDER_VIDEOS;
            case "PDF",   "PDFS"   -> SUBFOLDER_PDFS;
            case "FILE",  "FILES"  -> SUBFOLDER_FILES;
            default                -> SUBFOLDER_CONTENT;
        };
    }

    private String determineContentType(String filename) {
        if (filename == null || !filename.contains(".")) {
            return CONTENT_TYPE_OCTET;
        }
        return switch (filename.substring(filename.lastIndexOf(".") + 1).toLowerCase()) {
            case "jpg", "jpeg" -> CONTENT_TYPE_JPEG;
            case "png"         -> CONTENT_TYPE_PNG;
            case "gif"         -> CONTENT_TYPE_GIF;
            case "mp4"         -> CONTENT_TYPE_MP4;
            case "pdf"         -> CONTENT_TYPE_PDF;
            case "txt"         -> "text/plain";
            case "html", "htm" -> "text/html";
            case "css"         -> "text/css";
            case "js"          -> "application/javascript";
            case "json"        -> "application/json";
            case "xml"         -> "application/xml";
            case "zip"         -> "application/zip";
            case "doc", "docx" -> "application/msword";
            case "xls", "xlsx" -> "application/vnd.ms-excel";
            case "ppt", "pptx" -> "application/vnd.ms-powerpoint";
            default            -> CONTENT_TYPE_OCTET;
        };
    }
}
