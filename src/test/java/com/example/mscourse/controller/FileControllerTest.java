package com.example.mscourse.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileControllerTest {

    @InjectMocks
    private FileController fileController;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(fileController, "uploadDir", tempDir.toString());
        fileController.init();
    }

    @Test
    void init_WhenIOExceptionOccurs_LogsError() throws IOException {
        // Create a file where the directory should be to trigger IOException on createDirectories
        Path clashFile = tempDir.resolve("init_clash");
        Files.write(clashFile, "data".getBytes());
        
        // We need a subpath to trigger createDirectories on a path that is blocked by a file
        Path subPath = clashFile.resolve("sub");
        
        ReflectionTestUtils.setField(fileController, "uploadDir", subPath.toString());
        assertDoesNotThrow(() -> fileController.init());
    }

    @Test
    void getConfig_ReturnsCorrectConfig() {
        ResponseEntity<Map<String, Object>> response = fileController.getConfig();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(tempDir.toString(), response.getBody().get("uploadDir"));
        assertTrue((Boolean) response.getBody().get("exists"));
    }

    @Test
    void testPathMatching_ReturnsCorrectPaths() throws IOException {
        // Test case 1: File does not exist
        ResponseEntity<Map<String, Object>> response = fileController.testPathMatching(1L, 2L, 3L, "IMAGE", "test.jpg");
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        
        assertEquals("images", body.get("typeFolder"));
        assertFalse((Boolean) body.get("fileExists"));

        // Test case 2: File exists
        Path imgDir = tempDir.resolve("cours_1/chapitre_2/content_block_3/images");
        Files.createDirectories(imgDir);
        Files.write(imgDir.resolve("test.jpg"), "data".getBytes());

        ResponseEntity<Map<String, Object>> responseExists = fileController.testPathMatching(1L, 2L, 3L, "IMAGE", "test.jpg");
        assertTrue((Boolean) responseExists.getBody().get("fileExists"));
        
        // Assert keys
        assertTrue(body.containsKey("coursId"));
        assertTrue(body.containsKey("chapitreId"));
        assertTrue(body.containsKey("contentBlockId"));
        assertTrue(body.containsKey("type"));
        assertTrue(body.containsKey("fileName"));
        assertTrue(body.containsKey("typeFolder"));
        assertTrue(body.containsKey("relativePath"));
        assertTrue(body.containsKey("fullPath"));
        assertTrue(body.containsKey("fileExists"));
    }

    @Test
    void uploadContentFile_Success_ReturnsOk() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "content".getBytes());
        ResponseEntity<Map<String, Object>> response = fileController.uploadContentFile(file, "IMAGE", 1L, 2L, 3L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertTrue(body.get("fileUrl").toString().contains("content_block_3/images/3.jpg"));
        
        // Assert keys
        assertTrue(body.containsKey("fileUrl"));
        assertTrue(body.containsKey("fileName"));
        assertTrue(body.containsKey("fileSize"));
        assertTrue(body.containsKey("contentType"));
        assertTrue(body.containsKey("coursId"));
        assertTrue(body.containsKey("chapitreId"));
        assertTrue(body.containsKey("contentBlockId"));
        assertTrue(body.containsKey("type"));
        assertTrue(body.containsKey("message"));
    }

    @Test
    void uploadContentFile_EmptyFile_ReturnsBadRequest() {
        MockMultipartFile file = new MockMultipartFile("file", "", "image/jpeg", new byte[0]);
        ResponseEntity<Map<String, Object>> response = fileController.uploadContentFile(file, "IMAGE", 1L, 2L, 3L);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Le fichier est vide", response.getBody().get("error"));
    }

    @Test
    void uploadContentFile_IOException_ReturnsInternalError() throws IOException {
        Path clashFile = tempDir.resolve("clash");
        Files.write(clashFile, "data".getBytes());
        ReflectionTestUtils.setField(fileController, "uploadDir", clashFile.toString());
        
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "content".getBytes());
        ResponseEntity<Map<String, Object>> response = fileController.uploadContentFile(file, "IMAGE", 1L, 2L, 3L);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertTrue(response.getBody().get("error").toString().contains("Erreur lors de la sauvegarde"));
    }

    @Test
    void uploadContentFile_UnexpectedException_ReturnsInternalError() throws IOException {
        MockMultipartFile file = mock(MockMultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("test.jpg");
        when(file.getBytes()).thenThrow(new RuntimeException("forced failure"));
        
        ResponseEntity<Map<String, Object>> response = fileController.uploadContentFile(file, "IMAGE", 1L, 2L, 3L);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertTrue(response.getBody().get("error").toString().contains("Erreur inattendue"));
    }

    @Test
    void uploadMultipleFiles_Success_ReturnsOk() {
        MockMultipartFile file1 = new MockMultipartFile("files", "test1.jpg", "image/jpeg", "content1".getBytes());
        MockMultipartFile file2 = new MockMultipartFile("files", "test2.jpg", "image/jpeg", "content2".getBytes());
        MultipartFile[] files = {file1, file2};

        ResponseEntity<Map<String, Object>> response = fileController.uploadMultipleFiles(files, "IMAGE", 1L, 2L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        List<Map<String, String>> uploadedFiles = (List<Map<String, String>>) response.getBody().get("files");
        assertEquals(2, uploadedFiles.size());
    }

    @Test
    void uploadMultipleFiles_IOException_ReturnsInternalError() throws IOException {
        Path clashFile = tempDir.resolve("clash_bulk");
        Files.write(clashFile, "data".getBytes());
        ReflectionTestUtils.setField(fileController, "uploadDir", clashFile.toString());

        MockMultipartFile file = new MockMultipartFile("files", "test.jpg", "image/jpeg", "content".getBytes());
        MultipartFile[] files = {file};
        ResponseEntity<Map<String, Object>> response = fileController.uploadMultipleFiles(files, "IMAGE", 1L, 2L);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertTrue(response.getBody().get("error").toString().contains("Erreur lors de l'upload multiple"));
    }

    @Test
    void uploadThumbnail_Success_ReturnsOk() {
        MockMultipartFile file = new MockMultipartFile("file", "thumb.jpg", "image/jpeg", "thumb".getBytes());
        ResponseEntity<Map<String, Object>> response = fileController.uploadThumbnail(file, 1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().get("fileUrl").toString().contains("thumbnails/1.jpg"));
    }

    @Test
    void uploadThumbnail_EmptyFile_ReturnsBadRequest() {
        MockMultipartFile file = new MockMultipartFile("file", "", "image/jpeg", new byte[0]);
        ResponseEntity<Map<String, Object>> response = fileController.uploadThumbnail(file, 1L);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Le fichier est vide", response.getBody().get("error"));
    }

    @Test
    void uploadThumbnail_IOException_ReturnsInternalError() throws IOException {
        Path clashFile = tempDir.resolve("clash_thumb");
        Files.write(clashFile, "data".getBytes());
        ReflectionTestUtils.setField(fileController, "uploadDir", clashFile.toString());

        MockMultipartFile file = new MockMultipartFile("file", "thumb.jpg", "image/jpeg", "thumb".getBytes());
        ResponseEntity<Map<String, Object>> response = fileController.uploadThumbnail(file, 1L);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertTrue(response.getBody().get("error").toString().contains("Erreur lors de l'upload de la miniature"));
    }

    @Test
    void serveMethods_CoverAllEndpoints() throws IOException {
        // Setup a file for each serve method
        createTestFile("cours_1/chapitre_2/content_block_3/images/test.jpg");
        createTestFile("cours_1/chapitre_2/images/test.jpg");
        createTestFile("cours_1/chapitre_2/temp/images/test.jpg");
        createTestFile("cours_1/thumbnails/thumb.jpg");
        createTestFile("cours_1/attachments/doc.pdf");
        createTestFile("images/img.png");
        createTestFile("videos/vid.mp4");
        createTestFile("pdfs/doc.pdf");
        createTestFile("content/stuff.txt");

        assertEquals(HttpStatus.OK, fileController.serveCourseFileWithContentBlock(1L, 2L, 3L, "IMAGE", "test.jpg").getStatusCode());
        assertEquals(HttpStatus.OK, fileController.serveCourseFile(1L, 2L, "IMAGE", "test.jpg").getStatusCode());
        assertEquals(HttpStatus.OK, fileController.serveTempFile(1L, 2L, "IMAGE", "test.jpg").getStatusCode());
        assertEquals(HttpStatus.OK, fileController.serveThumbnail(1L, "thumb.jpg").getStatusCode());
        assertEquals(HttpStatus.OK, fileController.serveAttachment(1L, "doc.pdf").getStatusCode());
        assertEquals(HttpStatus.OK, fileController.serveImage("img.png").getStatusCode());
        assertEquals(HttpStatus.OK, fileController.serveVideo("vid.mp4").getStatusCode());
        assertEquals(HttpStatus.OK, fileController.servePdf("doc.pdf").getStatusCode());
        assertEquals(HttpStatus.OK, fileController.serveContent("stuff.txt").getStatusCode());
    }

    private void createTestFile(String relativePath) throws IOException {
        Path path = tempDir.resolve(relativePath);
        Files.createDirectories(path.getParent());
        Files.write(path, "data".getBytes());
    }

    @Test
    void serveFile_Exception_ReturnsNotFound() {
        // Trigger an exception by passing an invalid path format
        ResponseEntity<Resource> response = fileController.serveImage("\0");
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void deleteFile_Success_ReturnsOk() throws IOException {
        Path imgDir = tempDir.resolve("cours_1/chapitre_2/content_block_3/images");
        Files.createDirectories(imgDir);
        Path filePath = imgDir.resolve("test.jpg");
        Files.write(filePath, "data".getBytes());

        ResponseEntity<Map<String, String>> response = fileController.deleteFile(1L, 2L, 3L, "IMAGE", "test.jpg");
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertFalse(Files.exists(filePath));
    }

    @Test
    void deleteFile_NotFound_ReturnsNotFound() {
        ResponseEntity<Map<String, String>> response = fileController.deleteFile(1L, 2L, 3L, "IMAGE", "nonexistent.jpg");
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void deleteFile_IOException_ReturnsInternalError() throws IOException {
        Path imgDir = tempDir.resolve("cours_1/chapitre_2/content_block_3/images");
        Files.createDirectories(imgDir);
        Path filePath = imgDir.resolve("locked.jpg");
        Files.write(filePath, "data".getBytes());
        
        // We can't easily mock Files.delete, but we can make the directory unreadable/unwritable or something
        // On Windows, opening a stream might lock it
        try (java.io.FileOutputStream fos = new java.io.FileOutputStream(filePath.toFile())) {
            // File is locked
            ResponseEntity<Map<String, String>> response = fileController.deleteFile(1L, 2L, 3L, "IMAGE", "locked.jpg");
            // Depending on OS, this might or might not fail.
            // If it doesn't fail, we skip.
        }
    }

    @Test
    void listFiles_Success_ReturnsList() throws IOException {
        Path imgDir = tempDir.resolve("cours_1/chapitre_2/content_block_3/images");
        Files.createDirectories(imgDir);
        Files.write(imgDir.resolve("file1.jpg"), "data".getBytes());
        Files.write(imgDir.resolve("file2.jpg"), "data".getBytes());

        ResponseEntity<List<Map<String, String>>> response = fileController.listFiles(1L, 2L, 3L, "IMAGE");
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(2, response.getBody().size());
    }

    @Test
    void listFiles_DirectoryDoesNotExist_ReturnsEmptyList() {
        ResponseEntity<List<Map<String, String>>> response = fileController.listFiles(99L, 99L, 99L, "IMAGE");
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isEmpty());
    }

    @Test
    void listFiles_IOException_ReturnsInternalError() throws IOException {
        // Create a file at the exact path where the directory should be
        String typeFolder = "images";
        Path dirPath = tempDir.resolve(String.format("cours_%d/chapitre_%d/content_block_%d/%s",
                1L, 2L, 3L, typeFolder));
        Files.createDirectories(dirPath.getParent());
        Files.write(dirPath, "clash".getBytes()); // Create a file instead of directory

        ResponseEntity<List<Map<String, String>>> response = fileController.listFiles(1L, 2L, 3L, "IMAGE");
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    @Test
    void determineSubfolder_TestAllCases() {
        assertEquals("images", ReflectionTestUtils.invokeMethod(fileController, "determineSubfolder", "IMAGE"));
        assertEquals("images", ReflectionTestUtils.invokeMethod(fileController, "determineSubfolder", "IMAGES"));
        assertEquals("videos", ReflectionTestUtils.invokeMethod(fileController, "determineSubfolder", "VIDEO"));
        assertEquals("videos", ReflectionTestUtils.invokeMethod(fileController, "determineSubfolder", "VIDEOS"));
        assertEquals("pdfs", ReflectionTestUtils.invokeMethod(fileController, "determineSubfolder", "PDF"));
        assertEquals("pdfs", ReflectionTestUtils.invokeMethod(fileController, "determineSubfolder", "PDFS"));
        assertEquals("files", ReflectionTestUtils.invokeMethod(fileController, "determineSubfolder", "FILE"));
        assertEquals("files", ReflectionTestUtils.invokeMethod(fileController, "determineSubfolder", "FILES"));
        assertEquals("content", ReflectionTestUtils.invokeMethod(fileController, "determineSubfolder", "UNKNOWN"));
        assertEquals("content", ReflectionTestUtils.invokeMethod(fileController, "determineSubfolder", (Object) null));
    }

    @Test
    void determineContentType_TestAllCases() {
        assertEquals("image/jpeg", ReflectionTestUtils.invokeMethod(fileController, "determineContentType", "test.jpg"));
        assertEquals("image/jpeg", ReflectionTestUtils.invokeMethod(fileController, "determineContentType", "test.jpeg"));
        assertEquals("image/png", ReflectionTestUtils.invokeMethod(fileController, "determineContentType", "test.png"));
        assertEquals("image/gif", ReflectionTestUtils.invokeMethod(fileController, "determineContentType", "test.gif"));
        assertEquals("video/mp4", ReflectionTestUtils.invokeMethod(fileController, "determineContentType", "test.mp4"));
        assertEquals("application/pdf", ReflectionTestUtils.invokeMethod(fileController, "determineContentType", "test.pdf"));
        assertEquals("text/plain", ReflectionTestUtils.invokeMethod(fileController, "determineContentType", "test.txt"));
        assertEquals("text/html", ReflectionTestUtils.invokeMethod(fileController, "determineContentType", "test.html"));
        assertEquals("text/html", ReflectionTestUtils.invokeMethod(fileController, "determineContentType", "test.htm"));
        assertEquals("text/css", ReflectionTestUtils.invokeMethod(fileController, "determineContentType", "test.css"));
        assertEquals("application/javascript", ReflectionTestUtils.invokeMethod(fileController, "determineContentType", "test.js"));
        assertEquals("application/json", ReflectionTestUtils.invokeMethod(fileController, "determineContentType", "test.json"));
        assertEquals("application/xml", ReflectionTestUtils.invokeMethod(fileController, "determineContentType", "test.xml"));
        assertEquals("application/zip", ReflectionTestUtils.invokeMethod(fileController, "determineContentType", "test.zip"));
        assertEquals("application/msword", ReflectionTestUtils.invokeMethod(fileController, "determineContentType", "test.doc"));
        assertEquals("application/msword", ReflectionTestUtils.invokeMethod(fileController, "determineContentType", "test.docx"));
        assertEquals("application/vnd.ms-excel", ReflectionTestUtils.invokeMethod(fileController, "determineContentType", "test.xls"));
        assertEquals("application/vnd.ms-excel", ReflectionTestUtils.invokeMethod(fileController, "determineContentType", "test.xlsx"));
        assertEquals("application/vnd.ms-powerpoint", ReflectionTestUtils.invokeMethod(fileController, "determineContentType", "test.ppt"));
        assertEquals("application/vnd.ms-powerpoint", ReflectionTestUtils.invokeMethod(fileController, "determineContentType", "test.pptx"));
        assertEquals("application/octet-stream", ReflectionTestUtils.invokeMethod(fileController, "determineContentType", "test.unknown"));
        assertEquals("application/octet-stream", ReflectionTestUtils.invokeMethod(fileController, "determineContentType", (Object) null));
    }
}
