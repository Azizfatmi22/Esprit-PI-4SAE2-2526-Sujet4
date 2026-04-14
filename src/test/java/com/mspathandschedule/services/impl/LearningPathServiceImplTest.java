package com.mspathandschedule.services.impl;


import com.mspathandschedule.clients.CourseManagementFeignClient;
import com.mspathandschedule.clients.SessionManagementFeignClient;
import com.mspathandschedule.entities.LearningLevel;
import com.mspathandschedule.entities.LearningPath;
import com.mspathandschedule.repositories.LearningPathRepository;
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
class LearningPathServiceImplTest {

    @Mock
    private LearningPathRepository repository;

    @Mock
    private SessionManagementFeignClient sessionClient;

    @Mock
    private CourseManagementFeignClient courseClient;

    @InjectMocks
    private LearningPathServiceImpl learningPathService;

    private LearningPath testLearningPath;
    private Map<String, Object> testSession;
    private Map<String, Object> testPlanning;

    @BeforeEach
    void setUp() {
        testLearningPath = new LearningPath();
        testLearningPath.setId(1L);
        testLearningPath.setTitle("Java Mastery");
        testLearningPath.setDescription("Complete Java course");
        testLearningPath.setLevel(LearningLevel.valueOf("INTERMEDIATE"));
        testLearningPath.setSessionIds(new ArrayList<>());
        testLearningPath.setTotalHours(0);

        testPlanning = new HashMap<>();
        testPlanning.put("totalHours", 10);

        testSession = new HashMap<>();
        testSession.put("id", 100L);
        testSession.put("title", "Java Basics");
        testSession.put("planning", testPlanning);
    }

    @Test
    void createLearningPath_Success() {
        // Arrange
        when(repository.save(any(LearningPath.class))).thenReturn(testLearningPath);

        // Act
        LearningPath result = learningPathService.createLearningPath(testLearningPath);

        // Assert
        assertNotNull(result);
        assertEquals("Java Mastery", result.getTitle());
        verify(repository, times(1)).save(testLearningPath);
    }

    @Test
    void createLearningPath_WithSessions_CalculatesTotalHours() {
        // Arrange
        testLearningPath.setSessionIds(Arrays.asList(100L, 101L));

        Map<String, Object> session2 = new HashMap<>();
        Map<String, Object> planning2 = new HashMap<>();
        planning2.put("totalHours", 15);
        session2.put("planning", planning2);

        when(sessionClient.getSessionById(100L)).thenReturn(testSession);
        when(sessionClient.getSessionById(101L)).thenReturn(session2);
        when(repository.save(any(LearningPath.class))).thenReturn(testLearningPath);

        // Act
        LearningPath result = learningPathService.createLearningPath(testLearningPath);

        // Assert
        assertEquals(25, result.getTotalHours());
        verify(sessionClient, times(2)).getSessionById(anyLong());
    }

    @Test
    void getLearningPath_NotFound_ThrowsException() {
        // Arrange
        when(repository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> learningPathService.getLearningPath(999L));
        assertEquals("LearningPath not found", exception.getMessage());
    }

    @Test
    void getLearningPath_Found_ReturnsLearningPath() {
        // Arrange
        when(repository.findById(1L)).thenReturn(Optional.of(testLearningPath));

        // Act
        LearningPath result = learningPathService.getLearningPath(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void addSessionToPath_Success() {
        // Arrange
        when(repository.findById(1L)).thenReturn(Optional.of(testLearningPath));
        when(sessionClient.getSessionById(100L)).thenReturn(testSession);
        when(repository.save(any(LearningPath.class))).thenReturn(testLearningPath);

        // Act
        LearningPath result = learningPathService.addSessionToPath(1L, 100L);

        // Assert
        assertTrue(result.getSessionIds().contains(100L));
        assertEquals(10, result.getTotalHours());
    }

   /* @Test
    void addSessionToPath_SessionAlreadyExists_DoesNotAddDuplicate() {
        // Arrange
        testLearningPath.getSessionIds().add(100L);
        testLearningPath.setTotalHours(10);

        when(repository.findById(1L)).thenReturn(Optional.of(testLearningPath));

        // Act
        LearningPath result = learningPathService.addSessionToPath(1L, 100L);

        // Assert
        assertEquals(1, result.getSessionIds().size());
        assertEquals(10, result.getTotalHours());
        verify(sessionClient, never()).getSessionById(anyLong());
    }*/

    @Test
    void filterCoursesByLevel_Success() {
        // Arrange
        List<Map<String, Object>> mockCourses = new ArrayList<>();
        Map<String, Object> course1 = new HashMap<>();
        course1.put("id", 1L);
        course1.put("level", "BEGINNER");
        mockCourses.add(course1);

        when(courseClient.getCoursesByLevel("BEGINNER")).thenReturn(mockCourses);

        // Act
        List<Map<String, Object>> result = learningPathService.filterCoursesByLevel("BEGINNER");

        // Assert
        assertEquals(1, result.size());
        verify(courseClient, times(1)).getCoursesByLevel("BEGINNER");
    }

    @Test
    void filterCoursesByLevel_EmptyLevel_ReturnsEmptyList() {
        // Act
        List<Map<String, Object>> result = learningPathService.filterCoursesByLevel("");

        // Assert
        assertTrue(result.isEmpty());
        verify(courseClient, never()).getCoursesByLevel(anyString());
    }

    @Test
    void calculatePathComplexity_Success() {
        // Arrange
        testLearningPath.setSessionIds(Arrays.asList(100L, 101L));

        Map<String, Object> session2 = new HashMap<>();
        Map<String, Object> planning2 = new HashMap<>();
        planning2.put("totalHours", 12);
        session2.put("planning", planning2);
        session2.put("status", "ACTIVE");

        when(repository.findById(1L)).thenReturn(Optional.of(testLearningPath));
        when(sessionClient.getSessionById(100L)).thenReturn(testSession);
        when(sessionClient.getSessionById(101L)).thenReturn(session2);

        // Act
        Map<String, Object> result = learningPathService.calculatePathComplexity(1L);

        // Assert
        assertNotNull(result);
        assertTrue(result.containsKey("complexityScore"));
        assertTrue(result.containsKey("complexityLevel"));
        assertTrue(result.containsKey("totalSessions"));
        assertEquals(2, result.get("totalSessions"));
        assertEquals(22, result.get("totalHours"));
    }

    /*@Test
    void predictCompletionRate_Success() {
        // Arrange
        testLearningPath.setSessionIds(Arrays.asList(100L, 101L));

        Map<String, Object> session2 = new HashMap<>();
        Map<String, Object> planning2 = new HashMap<>();
        planning2.put("totalHours", 8);
        session2.put("planning", planning2);

        when(repository.findById(1L)).thenReturn(Optional.of(testLearningPath));
        when(sessionClient.getSessionById(100L)).thenReturn(testSession);
        when(sessionClient.getSessionById(101L)).thenReturn(session2);

        // Act
        Map<String, Object> result = learningPathService.predictCompletionRate(1L);

        // Assert
        assertNotNull(result);
        assertTrue(result.containsKey("predictedCompletionRate"));
        assertTrue(result.containsKey("riskLevel"));
        assertTrue((Double) result.get("predictedCompletionRate") >= 0);
        assertTrue((Double) result.get("predictedCompletionRate") <= 100);
    }
*/
    @Test
    void generateLearningSummary_ReturnsFormattedString() {
        // Arrange
        testLearningPath.setSessionIds(Arrays.asList(100L));
        testLearningPath.setObjectives("Master Java fundamentals");

        when(repository.findById(1L)).thenReturn(Optional.of(testLearningPath));
        when(sessionClient.getSessionById(100L)).thenReturn(testSession);

        // Act
        String summary = learningPathService.generateLearningSummary(1L);

        // Assert
        assertNotNull(summary);
        assertTrue(summary.contains("Java Mastery"));
        assertTrue(summary.contains("Master Java fundamentals"));
        assertTrue(summary.contains("1 sessions"));
        assertTrue(summary.contains("10 heures"));
    }

    @Test
    void getOptimalLearningOrder_ReturnsSortedSessions() {
        // Arrange
        testLearningPath.setSessionIds(Arrays.asList(100L, 101L));

        Map<String, Object> session2 = new HashMap<>();
        Map<String, Object> planning2 = new HashMap<>();
        planning2.put("totalHours", 5);
        session2.put("planning", planning2);

        when(repository.findById(1L)).thenReturn(Optional.of(testLearningPath));
        when(sessionClient.getSessionById(100L)).thenReturn(testSession);
        when(sessionClient.getSessionById(101L)).thenReturn(session2);

        // Act
        List<Map<String, Object>> result = learningPathService.getOptimalLearningOrder(1L);

        // Assert
        assertEquals(2, result.size());
        // First session should have fewer hours (5 vs 10)
        Map<String, Object> planning = (Map<String, Object>) result.get(0).get("planning");
        assertEquals(5, planning.get("totalHours"));
    }
}
