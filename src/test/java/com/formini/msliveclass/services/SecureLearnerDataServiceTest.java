package com.formini.msliveclass.services;

import com.formini.msliveclass.clients.CourseClient;
import com.formini.msliveclass.clients.EnrollmentClient;
import com.formini.msliveclass.dto.ChapterDTO;
import com.formini.msliveclass.dto.ContentBlockDTO;
import com.formini.msliveclass.dto.CourseDTO;
import com.formini.msliveclass.dto.PageResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecureLearnerDataServiceTest {

    @Mock
    private CourseClient courseClient;

    @Mock
    private EnrollmentClient enrollmentClient;

    @InjectMocks
    private SecureLearnerDataService secureLearnerDataService;

    private CourseDTO sampleCourse;

    @BeforeEach
    void setUp() {
        sampleCourse = new CourseDTO();
        sampleCourse.setId(1L);
        sampleCourse.setTitle("Java Programming");
        sampleCourse.setDescription("Learn Java from scratch");
        sampleCourse.setLevel("BEGINNER");
        sampleCourse.setDurationMinutes(120);
    }

    @Test
    void getAllAvailableCourses_Success() {
        PageResponse<CourseDTO> pageResponse = new PageResponse<>();
        pageResponse.setContent(Arrays.asList(sampleCourse));
        when(courseClient.getAllCourses(0, 100)).thenReturn(pageResponse);

        String result = secureLearnerDataService.getAllAvailableCourses();

        assertTrue(result.contains("Available Courses"));
        assertTrue(result.contains("Java Programming"));
        assertTrue(result.contains("2h 0m"));
    }

    @Test
    void getAllAvailableCourses_Empty() {
        PageResponse<CourseDTO> pageResponse = new PageResponse<>();
        pageResponse.setContent(Collections.emptyList());
        when(courseClient.getAllCourses(0, 100)).thenReturn(pageResponse);

        String result = secureLearnerDataService.getAllAvailableCourses();

        assertEquals("No courses are currently available on the platform.", result);
    }

    @Test
    void getAllAvailableCourses_Error() {
        when(courseClient.getAllCourses(0, 100)).thenThrow(new RuntimeException("API Down"));

        String result = secureLearnerDataService.getAllAvailableCourses();

        assertTrue(result.contains("trouble accessing the course catalog"));
    }

    @Test
    void getLearnerEnrollments_InvalidId() {
        assertEquals("I need to know who you are to show your enrollments. Please make sure you're logged in.", 
                     secureLearnerDataService.getLearnerEnrollments(null));
        assertEquals("I need to know who you are to show your enrollments. Please make sure you're logged in.", 
                     secureLearnerDataService.getLearnerEnrollments(" "));
    }

    @Test
    void getLearnerEnrollments_Success() {
        Map<String, Object> enrollment = new HashMap<>();
        enrollment.put("courseId", 1L);
        enrollment.put("status", "ACTIVE");
        enrollment.put("progress", 45.5);
        
        when(enrollmentClient.getEnrollmentsByLearnerId("learner-1")).thenReturn(Arrays.asList(enrollment));
        when(courseClient.getCourseTitle(1L)).thenReturn("Java Programming");

        String result = secureLearnerDataService.getLearnerEnrollments("learner-1");

        assertTrue(result.contains("Your Enrolled Courses"));
        assertTrue(result.contains("Java Programming"));
        assertTrue(result.contains("45.5%"));
    }

    @Test
    void getCourseContentForLearner_NotEnrolled() {
        when(enrollmentClient.getEnrollmentsByLearnerId("learner-1")).thenReturn(Collections.emptyList());

        String result = secureLearnerDataService.getCourseContentForLearner("learner-1", 1L);

        assertTrue(result.contains("need to enroll"));
    }

    @Test
    void getCourseContentForLearner_Success() {
        Map<String, Object> enrollment = new HashMap<>();
        enrollment.put("courseId", 1L);
        enrollment.put("status", "ACTIVE");
        when(enrollmentClient.getEnrollmentsByLearnerId("learner-1")).thenReturn(Arrays.asList(enrollment));
        
        sampleCourse.setChapters(Arrays.asList("Chapter 1: Intro"));
        when(courseClient.getCourseWithChapters(1L)).thenReturn(sampleCourse);

        String result = secureLearnerDataService.getCourseContentForLearner("learner-1", 1L);

        assertTrue(result.contains("Course: Java Programming"));
        assertTrue(result.contains("Course Chapters"));
    }

    @Test
    void getChapterContentForLearner_Success() {
        Map<String, Object> enrollment = new HashMap<>();
        enrollment.put("courseId", 1L);
        enrollment.put("status", "ACTIVE");
        when(enrollmentClient.getEnrollmentsByLearnerId("learner-1")).thenReturn(Arrays.asList(enrollment));

        ChapterDTO chapter = new ChapterDTO();
        chapter.setTitle("Getting Started");
        chapter.setDescription("Introduction to Java");
        
        ContentBlockDTO textBlock = new ContentBlockDTO();
        textBlock.setTitle("Hello World");
        textBlock.setType("TEXT");
        textBlock.setData("{\"text\":\"This is some text content\"}");
        
        ContentBlockDTO imgBlock = new ContentBlockDTO();
        imgBlock.setTitle("Diagram");
        imgBlock.setType("IMAGE");
        imgBlock.setData("{\"url\":\"http://image.com/1.png\"}");

        ContentBlockDTO vidBlock = new ContentBlockDTO();
        vidBlock.setTitle("Lesson 1");
        vidBlock.setType("VIDEO");
        vidBlock.setData("{\"url\":\"http://video.com/1.mp4\"}");

        ContentBlockDTO codeBlock = new ContentBlockDTO();
        codeBlock.setTitle("Java Code");
        codeBlock.setType("CODE");
        codeBlock.setData("{\"code\":\"public class Main {}\"}");

        chapter.setContentBlocks(Arrays.asList(textBlock, imgBlock, vidBlock, codeBlock));
        
        when(courseClient.getChapterWithContent(1L, 10L)).thenReturn(chapter);

        String result = secureLearnerDataService.getChapterContentForLearner("learner-1", 1L, 10L);

        assertTrue(result.contains("Chapter: Getting Started"));
        assertTrue(result.contains("This is some text content"));
        assertTrue(result.contains("image.com"));
        assertTrue(result.contains("video.com"));
        assertTrue(result.contains("public class Main"));
    }

    @Test
    void getLearnerProgressInCourse_Brackets() {
        Map<String, Object> enrollment = new HashMap<>();
        enrollment.put("courseId", 1L);
        enrollment.put("status", "ACTIVE");
        when(enrollmentClient.getEnrollmentsByLearnerId("learner-1")).thenReturn(Arrays.asList(enrollment));
        when(courseClient.getCourseTitle(1L)).thenReturn("Java");

        // Test 10%
        enrollment.put("progress", 10.0);
        assertTrue(secureLearnerDataService.getLearnerProgressInCourse("learner-1", 1L).contains("getting started"));

        // Test 40%
        enrollment.put("progress", 40.0);
        assertTrue(secureLearnerDataService.getLearnerProgressInCourse("learner-1", 1L).contains("quarter of the way"));

        // Test 60%
        enrollment.put("progress", 60.0);
        assertTrue(secureLearnerDataService.getLearnerProgressInCourse("learner-1", 1L).contains("over halfway"));

        // Test 90%
        enrollment.put("progress", 90.0);
        assertTrue(secureLearnerDataService.getLearnerProgressInCourse("learner-1", 1L).contains("Almost there"));

        // Test 100%
        enrollment.put("progress", 100.0);
        assertTrue(secureLearnerDataService.getLearnerProgressInCourse("learner-1", 1L).contains("Congratulations"));
    }

    @Test
    void recommendLearningPath_Goals() {
        assertTrue(secureLearnerDataService.recommendLearningPath("full stack", "l1").contains("Spring Boot Basics"));
        assertTrue(secureLearnerDataService.recommendLearningPath("backend", "l1").contains("Microservices"));
        assertTrue(secureLearnerDataService.recommendLearningPath("frontend", "l1").contains("Angular/React"));
        assertTrue(secureLearnerDataService.recommendLearningPath("other", "l1").contains("Start with fundamentals"));
    }

    @Test
    void getPublicLeaderboard_Success() {
        Map<String, Object> e1 = new HashMap<>();
        e1.put("learnerId", "l1");
        e1.put("status", "COMPLETED");
        e1.put("progress", 100.0);

        Map<String, Object> e2 = new HashMap<>();
        e2.put("learnerId", "l2");
        e2.put("status", "ACTIVE");
        e2.put("progress", 50.0);

        when(enrollmentClient.getAllEnrollments()).thenReturn(Arrays.asList(e1, e2));

        String result = secureLearnerDataService.getPublicLeaderboard(5);

        assertTrue(result.contains("Top 5 Learners"));
        assertTrue(result.contains("Learner l1"));
        assertTrue(result.contains("🥇"));
    }
}
