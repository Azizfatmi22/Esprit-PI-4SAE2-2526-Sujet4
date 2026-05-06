// FileController.java
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

import jakarta.annotation.PostConstruct;  // Au lieu de javax.annotation.PostConstruct
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
     * Debug endpoint to check upload directory configuration
     */
    @GetMapping("/debug/config")
    public ResponseEntity<?> getConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("uploadDir", uploadDir);
        config.put("absolutePath", Paths.get(uploadDir).toAbsolutePath().toString());
        config.put("exists", Files.exists(Paths.get(uploadDir)));
        
        // Check if the specific file exists
        Path testFile = Paths.get(uploadDir, "cours_3/chapitre_3/content_block_4/images/4.jpg");
        config.put("testFileExists", Files.exists(testFile));
        config.put("testFilePath", testFile.toAbsolutePath().toString());
        
        return ResponseEntity.ok(config);
    }

    /**
     * Debug endpoint to test path matching
     */
    @GetMapping("/debug/test/{coursId}/{chapitreId}/{contentBlockId}/{type}/{filename:.+}")
    public ResponseEntity<?> testPathMatching(

            @PathVariable Long coursId,
            @PathVariable Long chapitreId,
            @PathVariable Long contentBlockId,
            @PathVariable String type,
            @PathVariable String filename) {
        
        Map<String, Object> result = new HashMap<>();
        result.put("coursId", coursId);
        result.put("chapitreId", chapitreId);
        result.put("contentBlockId", contentBlockId);
        result.put("type", type);
        result.put("filename", filename);
        
        String typeFolder = determineSubfolder(type);
        result.put("typeFolder", typeFolder);
        
        String relativePath = String.format("cours_%d/chapitre_%d/content_block_%d/%s/%s",
                coursId, chapitreId, contentBlockId, typeFolder, filename);
        result.put("relativePath", relativePath);
        
        Path fullPath = Paths.get(uploadDir, relativePath);
        result.put("fullPath", fullPath.toAbsolutePath().toString());
        result.put("fileExists", Files.exists(fullPath));
        
        return ResponseEntity.ok(result);
    }

    // ==================== UPLOAD METHODS ====================

    /**
     * Upload content file with course and chapter structure
     * Files are named by content block ID: {id}.ext (e.g., 19.jpg)
     */
    @PostMapping("/content")
    public ResponseEntity<?> uploadContentFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("type") String type,
            @RequestParam("coursId") Long coursId,
            @RequestParam("chapitreId") Long chapitreId,
            @RequestParam("contentBlockId") Long contentBlockId) {  // ← Required

        Map<String, Object> response = new HashMap<>();

        try {
            // Validation
            if (file.isEmpty()) {
                response.put("error", "Le fichier est vide");
                return ResponseEntity.badRequest().body(response);
            }

            // Get file extension
            String originalFilename = file.getOriginalFilename();
            String extension = "";

            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }

            // Determine subfolder based on type
            String typeFolder = determineSubfolder(type);

            // Filename: {id}.ext (e.g., 19.jpg)
            String filename = contentBlockId + extension;

            // Save with content block ID
            String uploadPath = String.format("%s/cours_%d/chapitre_%d/content_block_%d/%s/",
                    uploadDir, coursId, chapitreId, contentBlockId, typeFolder);
            String fileUrl = String.format("/api/courses/uploads/cours_%d/chapitre_%d/content_block_%d/%s/%s",
                    coursId, chapitreId, contentBlockId, typeFolder, filename);

            Path dirPath = Paths.get(uploadPath);
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
                logger.info("Created directory: {}", dirPath);
            }

            // Save the file (overwrite if exists)
            Path filePath = Paths.get(uploadPath + filename);
            Files.write(filePath, file.getBytes());
            logger.info("File saved: {}", filePath);

            response.put("fileUrl", fileUrl);
            response.put("fileName", originalFilename);
            response.put("fileSize", file.getSize());
            response.put("contentType", file.getContentType());
            response.put("coursId", coursId);
            response.put("chapitreId", chapitreId);
            response.put("contentBlockId", contentBlockId);
            response.put("type", typeFolder);
            response.put("message", "Fichier uploadé avec succès");

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            logger.error("IO Error during file upload", e);
            response.put("error", "Erreur lors de la sauvegarde: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        } catch (Exception e) {
            logger.error("Unexpected error during file upload", e);
            response.put("error", "Erreur inattendue: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Upload multiple files
     */
    @PostMapping("/content/bulk")
    public ResponseEntity<?> uploadMultipleFiles(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam("type") String type,
            @RequestParam("coursId") Long coursId,
            @RequestParam("chapitreId") Long chapitreId
    ) {

        Map<String, Object> response = new HashMap<>();
        List<Map<String, String>> uploadedFiles = new ArrayList<>();

        try {
            for (MultipartFile file : files) {
                if (!file.isEmpty()) {
                    String originalFilename = file.getOriginalFilename();
                    String extension = "";
                    String nameWithoutExtension = originalFilename;
                    
                    if (originalFilename != null && originalFilename.contains(".")) {
                        extension = originalFilename.substring(originalFilename.lastIndexOf("."));
                        nameWithoutExtension = originalFilename.substring(0, originalFilename.lastIndexOf("."));
                    }

                    String safeFilename = nameWithoutExtension != null ?
                            nameWithoutExtension.replaceAll("[^a-zA-Z0-9.-]", "_") : "file";
                    String filename = System.currentTimeMillis() + "_" + safeFilename + extension;

                    String typeFolder = determineSubfolder(type);
                    String uploadPath = String.format("%s/cours_%d/chapitre_%d/%s/",
                            uploadDir, coursId, chapitreId, typeFolder);

                    Path dirPath = Paths.get(uploadPath);
                    if (!Files.exists(dirPath)) {
                        Files.createDirectories(dirPath);
                    }

                    Path filePath = Paths.get(uploadPath + filename);
                    Files.write(filePath, file.getBytes());

                    String fileUrl = String.format("/api/courses/uploads/cours_%d/chapitre_%d/%s/%s",
                            coursId, chapitreId, typeFolder, filename);

                    Map<String, String> fileInfo = new HashMap<>();
                    fileInfo.put("fileUrl", fileUrl);
                    fileInfo.put("fileName", originalFilename != null ? originalFilename : filename);
                    fileInfo.put("fileSize", String.valueOf(file.getSize()));

                    uploadedFiles.add(fileInfo);
                }
            }

            response.put("files", uploadedFiles);
            response.put("count", uploadedFiles.size());
            response.put("type", type);
            response.put("coursId", coursId);
            response.put("chapitreId", chapitreId);

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            logger.error("IO Error during bulk upload", e);
            response.put("error", "Erreur lors de l'upload multiple: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Upload thumbnail
     * Filename: {id}.ext (e.g., 11.jpg)
     */
    @PostMapping("/thumbnails/upload")
    public ResponseEntity<?> uploadThumbnail(
            @RequestParam("file") MultipartFile file,
            @RequestParam("coursId") Long coursId) {

        Map<String, Object> response = new HashMap<>();

        try {
            if (file.isEmpty()) {
                response.put("error", "Le fichier est vide");
                return ResponseEntity.badRequest().body(response);
            }

            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }

            // Filename: {id}.ext (e.g., 11.jpg)
            String filename = coursId + extension;

            // Structure: uploads/cours_{coursId}/thumbnails/
            String uploadPath = String.format("%s/cours_%d/thumbnails/", uploadDir, coursId);
            Path dirPath = Paths.get(uploadPath);
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
                logger.info("Created directory: {}", dirPath);
            }

            // Save the file (overwrite if exists)
            Path filePath = Paths.get(uploadPath + filename);
            Files.write(filePath, file.getBytes());
            logger.info("Thumbnail saved: {}", filePath);

            // Fix the URL formatting - use String.format instead of concatenation
            String fileUrl = String.format("/api/courses/uploads/cours_%d/thumbnails/%s",
                    coursId, filename);

            response.put("fileUrl", fileUrl);
            response.put("fileName", originalFilename);
            response.put("fileSize", file.getSize());
            response.put("coursId", coursId);
            response.put("message", "Thumbnail uploadé avec succès");

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            logger.error("IO Error during thumbnail upload", e);
            response.put("error", "Erreur lors de l'upload de la miniature: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    // ==================== SERVE METHODS ====================

    /**
     * Serve files with course/chapter/content_block structure
     * Matches: /api/courses/uploads/cours_{coursId}/chapitre_{chapitreId}/content_block_{contentBlockId}/{type}/{filename}
     */
    @GetMapping("/cours_{coursId}/chapitre_{chapitreId}/content_block_{contentBlockId}/{type}/{filename:.+}")
    public ResponseEntity<Resource> serveCourseFileWithContentBlock(
            @PathVariable Long coursId,
            @PathVariable Long chapitreId,
            @PathVariable Long contentBlockId,
            @PathVariable String type,
            @PathVariable String filename) {

        logger.info("=== SERVE CONTENT BLOCK FILE ===");
        logger.info("coursId: {}", coursId);
        logger.info("chapitreId: {}", chapitreId);
        logger.info("contentBlockId: {}", contentBlockId);
        logger.info("type: {}", type);
        logger.info("filename: {}", filename);
        String typeFolder = determineSubfolder(type);
        logger.info("typeFolder: {}", typeFolder);
        
        String filePath = String.format("cours_%d/chapitre_%d/content_block_%d/%s/%s",
                coursId, chapitreId, contentBlockId, typeFolder, filename);
        logger.info("Constructed relativePath: {}", filePath);

        return serveFile(filePath, filename);
    }

    /**
     * Serve files with course/chapter structure (legacy/temp support)
     * Matches: /api/courses/uploads/cours_{coursId}/chapitre_{chapitreId}/{type}/{filename}
     */
    @GetMapping("/cours_{coursId}/chapitre_{chapitreId}/{type}/{filename:.+}")
    public ResponseEntity<Resource> serveCourseFile(
            @PathVariable Long coursId,
            @PathVariable Long chapitreId,
            @PathVariable String type,
            @PathVariable String filename) {

        logger.info("=== SERVE COURSE FILE ===");
        logger.info("coursId: {}", coursId);
        logger.info("chapitreId: {}", chapitreId);
        logger.info("type: {}", type);
        logger.info("filename: {}", filename);

        String typeFolder = determineSubfolder(type);
        String filePath = String.format("cours_%d/chapitre_%d/%s/%s",
                coursId, chapitreId, typeFolder, filename);

        return serveFile(filePath, filename);
    }

    /**
     * Serve temp files (fallback for files uploaded without content block ID)
     * Matches: /api/courses/uploads/cours_{coursId}/chapitre_{chapitreId}/temp/{type}/{filename}
     */
    @GetMapping("/cours_{coursId}/chapitre_{chapitreId}/temp/{type}/{filename:.+}")
    public ResponseEntity<Resource> serveTempFile(
            @PathVariable Long coursId,
            @PathVariable Long chapitreId,
            @PathVariable String type,
            @PathVariable String filename) {

        logger.info("=== SERVE TEMP FILE ===");
        logger.info("coursId: {}", coursId);
        logger.info("chapitreId: {}", chapitreId);
        logger.info("type: {}", type);
        logger.info("filename: {}", filename);

        String typeFolder = determineSubfolder(type);
        String filePath = String.format("cours_%d/chapitre_%d/temp/%s/%s",
                coursId, chapitreId, typeFolder, filename);

        return serveFile(filePath, filename);
    }

    /**
     * Serve thumbnails
     * Matches: /api/courses/uploads/{coursId}/thumbnails/{filename}
     */
    @GetMapping("/{coursId}/thumbnails/{filename:.+}")
    public ResponseEntity<Resource> serveThumbnail(
            @PathVariable Long coursId,
            @PathVariable String filename) {

        logger.info("=== SERVE COURSE THUMBNAIL ===");
        logger.info("coursId: {}", coursId);
        logger.info("filename: {}", filename);

        // ✅ Map URL path to actual storage path structure with cours_ prefix
        String filePath = String.format("cours_%d/thumbnails/%s", coursId, filename);

        return serveFile(filePath, filename);
    }

    /**
     * Serve course attachments
     */
    @GetMapping("/cours_{coursId}/attachments/{filename:.+}")
    public ResponseEntity<Resource> serveAttachment(
            @PathVariable Long coursId,
            @PathVariable String filename) {
        
        String filePath = String.format("cours_%d/attachments/%s", coursId, filename);
        return serveFile(filePath, filename);
    }

    /**
     * Serve images (legacy support)
     */
    @GetMapping("/images/{filename:.+}")
    public ResponseEntity<Resource> serveImage(@PathVariable String filename) {
        return serveFile("images/" + filename, filename);
    }

    /**
     * Serve videos (legacy support)
     */
    @GetMapping("/videos/{filename:.+}")
    public ResponseEntity<Resource> serveVideo(@PathVariable String filename) {
        return serveFile("videos/" + filename, filename);
    }

    /**
     * Serve PDFs (legacy support)
     */
    @GetMapping("/pdfs/{filename:.+}")
    public ResponseEntity<Resource> servePdf(@PathVariable String filename) {
        return serveFile("pdfs/" + filename, filename);
    }

    /**
     * Serve content files (legacy support)
     */
    @GetMapping("/content/{filename:.+}")
    public ResponseEntity<Resource> serveContent(@PathVariable String filename) {
        return serveFile("content/" + filename, filename);
    }

    /**
     * Generic method to serve files
     */
    private ResponseEntity<Resource> serveFile(String relativePath, String filename) {
        try {
            Path filePath = Paths.get(uploadDir, relativePath);
            logger.info("=== SERVE FILE DEBUG ===");
            logger.info("uploadDir: {}", uploadDir);
            logger.info("relativePath: {}", relativePath);
            logger.info("filename: {}", filename);
            logger.info("Constructed filePath: {}", filePath);
            logger.info("Absolute path: {}", filePath.toAbsolutePath());
            logger.info("File exists: {}", Files.exists(filePath));
            logger.info("File readable: {}", Files.isReadable(filePath));

            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                String contentType = determineContentType(filename);
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            } else {
                logger.warn("File not found or not readable: {}", filePath);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("Error serving file: {}", relativePath, e);
            return ResponseEntity.notFound().build();
        }
    }

    // ==================== HELPER METHODS ====================

    /**
     * Determine subfolder based on file type
     */
    private String determineSubfolder(String type) {
        if (type == null) return "content";

        switch (type.toUpperCase()) {
            case "IMAGE":
            case "IMAGES":
                return "images";
            case "VIDEO":
            case "VIDEOS":
                return "videos";
            case "PDF":
            case "PDFS":
                return "pdfs";
            case "FILE":
            case "FILES":
                return "files";
            default:
                return "content";
        }
    }

    /**
     * Determine content type based on file extension
     */
    private String determineContentType(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "application/octet-stream";
        }

        String extension = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();

        switch (extension) {
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "png":
                return "image/png";
            case "gif":
                return "image/gif";
            case "mp4":
                return "video/mp4";
            case "pdf":
                return "application/pdf";
            case "txt":
                return "text/plain";
            case "html":
            case "htm":
                return "text/html";
            case "css":
                return "text/css";
            case "js":
                return "application/javascript";
            case "json":
                return "application/json";
            case "xml":
                return "application/xml";
            case "zip":
                return "application/zip";
            case "doc":
            case "docx":
                return "application/msword";
            case "xls":
            case "xlsx":
                return "application/vnd.ms-excel";
            case "ppt":
            case "pptx":
                return "application/vnd.ms-powerpoint";
            default:
                return "application/octet-stream";
        }
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Delete a file
     */
    @DeleteMapping("/cours_{coursId}/chapitre_{chapitreId}/content_block_{contentBlockId}/{type}/{filename:.+}")
    public ResponseEntity<?> deleteFile(
            @PathVariable Long coursId,
            @PathVariable Long chapitreId,
            @PathVariable Long contentBlockId,
            @PathVariable String type,
            @PathVariable String filename) {

        try {

            String typeFolder = determineSubfolder(type);
            String filePath = String.format("%s/cours_%d/chapitre_%d/content_block_%d/%s/%s",
                    uploadDir,coursId, chapitreId, contentBlockId, typeFolder, filename);



            Path path = Paths.get(filePath);
            if (Files.exists(path)) {
                Files.delete(path);
                logger.info("File deleted: {}", path);
                return ResponseEntity.ok(Map.of("message", "Fichier supprimé avec succès"));
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (IOException e) {
            logger.error("Error deleting file", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur lors de la suppression: " + e.getMessage()));
        }
    }

    /**
     * List files in a course/chapter
     */
    @GetMapping("/cours_{coursId}/chapitre_{chapitreId}/content_block_{contentBlockId}/{type}/list")
    public ResponseEntity<?> listFiles(
            @PathVariable Long coursId,
            @PathVariable Long chapitreId,
            @PathVariable Long contentBlockId,
            @PathVariable String type) {

        try {
            String typeFolder = determineSubfolder(type);
            String dirPath = String.format("%s/cours_%d/chapitre_%d/content_block_%d/%s/",
                    uploadDir, coursId, chapitreId,contentBlockId, typeFolder);

            Path path = Paths.get(dirPath);
            if (!Files.exists(path)) {
                return ResponseEntity.ok(List.of());
            }

            List<Map<String, String>> files = new ArrayList<>();
            try (java.util.stream.Stream<Path> stream = Files.list(path)) {
                stream.forEach(filePath -> {
                    Map<String, String> fileInfo = new HashMap<>();
                    String filename = filePath.getFileName().toString();
                    fileInfo.put("fileName", filename);
                    fileInfo.put("fileUrl", String.format("/api/courses/uploads/cours_%d/chapitre_%d/content_block_%d/%s/%s",
                            coursId, chapitreId,contentBlockId, typeFolder, filename));
                    try {
                        fileInfo.put("fileSize", String.valueOf(Files.size(filePath)));
                    } catch (IOException e) {
                        fileInfo.put("fileSize", "0");
                    }
                    files.add(fileInfo);
                });
            }

            return ResponseEntity.ok(files);

        } catch (IOException e) {
            logger.error("Error listing files", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur lors du listage des fichiers: " + e.getMessage()));
        }
    }
}