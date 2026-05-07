package com.example.mscourse.services.impl;

import com.example.mscourse.dto.CourseAttachmentDTO;
import com.example.mscourse.dto.CreateAttachmentRequestDTO;
import com.example.mscourse.dto.UpdateAttachmentRequestDTO;
import com.example.mscourse.entities.AttachmentCategory;
import com.example.mscourse.entities.Course;
import com.example.mscourse.entities.CourseAttachment;
import com.example.mscourse.exceptions.ResourceNotFoundException;
import com.example.mscourse.exceptions.ValidationException;
import com.example.mscourse.repositories.CourseAttachmentRepository;
import com.example.mscourse.repositories.CourseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseAttachmentServiceImplTest {

    @Mock
    private CourseAttachmentRepository attachmentRepository;

    @Mock
    private CourseRepository courseRepository;

    @InjectMocks
    private CourseAttachmentServiceImpl attachmentService;

    @TempDir
    Path tempDir;

    private Course testCourse;
    private CourseAttachment testAttachment;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(attachmentService, "uploadDir", tempDir.toString());
        
        testCourse = new Course();
        testCourse.setId(1L);
        testCourse.setTitle("Test Course");
        testCourse.setAttachments(new ArrayList<>());

        testAttachment = new CourseAttachment();
        testAttachment.setId(10L);
        testAttachment.setFileName("test.pdf");
        testAttachment.setFileType("application/pdf");
        testAttachment.setFileSize(1024L);
        testAttachment.setFileUrl("/api/courses/uploads/cours_1/attachments/test.pdf");
        testAttachment.setCategory(AttachmentCategory.RESOURCES);
        testAttachment.setCourse(testCourse);
    }

    @Test
    void createAttachment_Success() {
        CreateAttachmentRequestDTO request = new CreateAttachmentRequestDTO();
        request.setFileName("manual.pdf");
        request.setCategory(AttachmentCategory.SYLLABUS);

        when(courseRepository.findById(1L)).thenReturn(Optional.of(testCourse));
        when(attachmentRepository.save(any(CourseAttachment.class))).thenAnswer(i -> {
            CourseAttachment a = i.getArgument(0);
            a.setId(20L);
            return a;
        });

        CourseAttachmentDTO result = attachmentService.createAttachment(1L, request);

        assertNotNull(result);
        assertEquals(20L, result.getId());
        assertEquals("manual.pdf", result.getFileName());
        verify(courseRepository).findById(1L);
        verify(attachmentRepository).save(any(CourseAttachment.class));
    }

    @Test
    void uploadAttachment_Success() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf", "dummy content".getBytes());
        
        when(courseRepository.findById(1L)).thenReturn(Optional.of(testCourse));
        when(attachmentRepository.save(any(CourseAttachment.class))).thenReturn(testAttachment);

        CourseAttachmentDTO result = attachmentService.uploadAttachment(1L, file, AttachmentCategory.RESOURCES, "Desc");

        assertNotNull(result);
        verify(attachmentRepository).save(any(CourseAttachment.class));
        assertTrue(Files.exists(tempDir.resolve("cours_1/attachments/test.pdf")));
    }

    @Test
    void getAttachmentById_Success() {
        when(attachmentRepository.findById(10L)).thenReturn(Optional.of(testAttachment));
        
        CourseAttachmentDTO result = attachmentService.getAttachmentById(10L);
        
        assertNotNull(result);
        assertEquals(10L, result.getId());
    }

    @Test
    void getAttachmentById_NotFound() {
        when(attachmentRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> attachmentService.getAttachmentById(99L));
    }

    @Test
    void getAttachmentsByCourse_Success() {
        when(attachmentRepository.findByCourseId(1L)).thenReturn(List.of(testAttachment));
        List<CourseAttachmentDTO> result = attachmentService.getAttachmentsByCourse(1L);
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
    }

    @Test
    void updateAttachment_Success() {
        UpdateAttachmentRequestDTO request = new UpdateAttachmentRequestDTO();
        request.setDescription("Updated Description");
        
        when(attachmentRepository.findById(10L)).thenReturn(Optional.of(testAttachment));
        when(attachmentRepository.save(any(CourseAttachment.class))).thenReturn(testAttachment);

        CourseAttachmentDTO result = attachmentService.updateAttachment(10L, request);
        
        assertNotNull(result);
        verify(attachmentRepository).save(testAttachment);
    }

    @Test
    void updateAttachmentFile_Success() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "new.pdf", "application/pdf", "new content".getBytes());
        
        when(attachmentRepository.findById(10L)).thenReturn(Optional.of(testAttachment));
        when(attachmentRepository.save(any(CourseAttachment.class))).thenReturn(testAttachment);

        CourseAttachmentDTO result = attachmentService.updateAttachmentFile(1L, 10L, file, AttachmentCategory.SYLLABUS, "New Desc");

        assertNotNull(result);
        verify(attachmentRepository).save(testAttachment);
    }

    @Test
    void deleteAttachment_Success() throws IOException {
        // Create the file first so deletePhysicalFile can find it
        Path filePath = tempDir.resolve("cours_1/attachments/test.pdf");
        Files.createDirectories(filePath.getParent());
        Files.write(filePath, "data".getBytes());

        when(attachmentRepository.findById(10L)).thenReturn(Optional.of(testAttachment));
        
        attachmentService.deleteAttachment(10L);
        
        verify(attachmentRepository).delete(testAttachment);
        assertFalse(Files.exists(filePath));
    }

    @Test
    void downloadAttachment_Success() throws IOException {
        Path filePath = tempDir.resolve("cours_1/attachments/test.pdf");
        Files.createDirectories(filePath.getParent());
        Files.write(filePath, "secret content".getBytes());

        when(attachmentRepository.findById(10L)).thenReturn(Optional.of(testAttachment));
        
        byte[] result = attachmentService.downloadAttachment(10L);
        
        assertArrayEquals("secret content".getBytes(), result);
    }

    @Test
    void downloadAttachment_FileNotFoundOnDisk() {
        when(attachmentRepository.findById(10L)).thenReturn(Optional.of(testAttachment));
        assertThrows(ResourceNotFoundException.class, () -> attachmentService.downloadAttachment(10L));
    }

    @Test
    void getTotalAttachmentsSize_Success() {
        when(attachmentRepository.getTotalSizeByCourseId(1L)).thenReturn(5000L);
        Long result = attachmentService.getTotalAttachmentsSize(1L);
        assertEquals(5000L, result);
    }

    @Test
    void getAttachmentStatistics_Success() {
        List<Object[]> stats = new ArrayList<>();
        stats.add(new Object[]{AttachmentCategory.RESOURCES, 5L, 10000L});
        when(attachmentRepository.getAttachmentStatisticsByCourse(1L)).thenReturn(stats);
        List<Object[]> result = attachmentService.getAttachmentStatistics(1L);
        assertEquals(1, result.size());
    }

    @Test
    void validateMultipartFile_TooLarge() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(600L * 1024 * 1024); // 600MB
        
        assertThrows(ValidationException.class, () -> ReflectionTestUtils.invokeMethod(attachmentService, "validateMultipartFile", file));
    }

    @Test
    void resolveAttachmentPath_Validations() {
        // Test various path patterns
        assertNotNull(ReflectionTestUtils.invokeMethod(attachmentService, "resolveAttachmentPath", "/api/courses/uploads/c1/a.pdf"));
        assertNotNull(ReflectionTestUtils.invokeMethod(attachmentService, "resolveAttachmentPath", "/uploads/c1/a.pdf"));
        assertNotNull(ReflectionTestUtils.invokeMethod(attachmentService, "resolveAttachmentPath", "c1/a.pdf"));
        
        assertThrows(Exception.class, () -> ReflectionTestUtils.invokeMethod(attachmentService, "resolveAttachmentPath", "   "));
    }
}
