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
    void getConfig_ReturnsCorrectConfig() {
        ResponseEntity<Map<String, Object>> response = fileController.getConfig();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(tempDir.toString(), response.getBody().get("uploadDir"));
        assertTrue((Boolean) response.getBody().get("exists"));
    }

    @Test
    void testPathMatching_ReturnsCorrectPaths() {
        ResponseEntity<Map<String, Object>> response = fileController.testPathMatching(1L, 2L, 3L, "IMAGE", "test.jpg");
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertEquals("images", body.get("typeFolder"));
        assertTrue(body.get("relativePath").toString().contains("cours_1/chapitre_2/content_block_3/images/test.jpg"));
    }

    @Test
    void uploadContentFile_Success_ReturnsOk() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "content".getBytes());
        ResponseEntity<Map<String, Object>> response = fileController.uploadContentFile(file, "IMAGE", 1L, 2L, 3L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().get("fileUrl").toString().contains("content_block_3/images/3.jpg"));
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
        // Create a file at the path where a directory is expected to trigger an IOException
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
    void uploadThumbnail_Success_ReturnsOk() {
        MockMultipartFile file = new MockMultipartFile("file", "thumb.jpg", "image/jpeg", "thumb".getBytes());
        ResponseEntity<Map<String, Object>> response = fileController.uploadThumbnail(file, 1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().get("fileUrl").toString().contains("thumbnails/1.jpg"));
    }

    @Test
    void serveFile_Success_ReturnsResource() throws IOException {
        Path filePath = tempDir.resolve("test.txt");
        Files.write(filePath, "hello".getBytes());
        
        ResponseEntity<Resource> response = fileController.serveImage("../test.txt"); // Hacky way to test serveFile via one of the public endpoints
        // Actually, serveImage calls serveFile("images/" + filename, filename)
        // Let's create the file in the expected location
        Path imgDir = tempDir.resolve("images");
        Files.createDirectories(imgDir);
        Files.write(imgDir.resolve("test.jpg"), "data".getBytes());

        ResponseEntity<Resource> response2 = fileController.serveImage("test.jpg");
        assertEquals(HttpStatus.OK, response2.getStatusCode());
        assertEquals("image/jpeg", response2.getHeaders().getContentType().toString());
    }

    @Test
    void serveFile_NotFound_ReturnsNotFound() {
        ResponseEntity<Resource> response = fileController.serveImage("nonexistent.jpg");
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
    void determineSubfolder_TestAllCases() {
        assertEquals("images", ReflectionTestUtils.invokeMethod(fileController, "determineSubfolder", "IMAGE"));
        assertEquals("videos", ReflectionTestUtils.invokeMethod(fileController, "determineSubfolder", "VIDEO"));
        assertEquals("pdfs", ReflectionTestUtils.invokeMethod(fileController, "determineSubfolder", "PDF"));
        assertEquals("files", ReflectionTestUtils.invokeMethod(fileController, "determineSubfolder", "FILE"));
        assertEquals("content", ReflectionTestUtils.invokeMethod(fileController, "determineSubfolder", "UNKNOWN"));
        assertEquals("content", ReflectionTestUtils.invokeMethod(fileController, "determineSubfolder", (Object) null));
    }

    @Test
    void determineContentType_TestCommonExtensions() {
        assertEquals("image/jpeg", ReflectionTestUtils.invokeMethod(fileController, "determineContentType", "test.jpg"));
        assertEquals("video/mp4", ReflectionTestUtils.invokeMethod(fileController, "determineContentType", "test.mp4"));
        assertEquals("application/pdf", ReflectionTestUtils.invokeMethod(fileController, "determineContentType", "test.pdf"));
        assertEquals("application/json", ReflectionTestUtils.invokeMethod(fileController, "determineContentType", "test.json"));
        assertEquals("application/octet-stream", ReflectionTestUtils.invokeMethod(fileController, "determineContentType", "test.unknown"));
        assertEquals("application/octet-stream", ReflectionTestUtils.invokeMethod(fileController, "determineContentType", "noextension"));
    }
}
