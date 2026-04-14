package com.formini.msliveclass.services;

import com.formini.msliveclass.clients.CourseClient;
import com.formini.msliveclass.clients.EnrollmentClient;
import com.formini.msliveclass.dto.ChapterDTO;
import com.formini.msliveclass.dto.ContentBlockDTO;
import com.formini.msliveclass.dto.CourseDTO;
import com.formini.msliveclass.dto.PageResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;

/**
 * AI Function Service - Provides callable functions for the AI model
 * These functions allow the AI to query the database directly
 */
@Service
public class AIFunctionService {

    private static final Logger logger = LoggerFactory.getLogger(AIFunctionService.class);

    private final CourseClient courseClient;
    private final EnrollmentClient enrollmentClient;

    @Autowired
    public AIFunctionService(CourseClient courseClient, EnrollmentClient enrollmentClient) {
        this.courseClient = courseClient;
        this.enrollmentClient = enrollmentClient;
    }

    /**
     * Function: Get all available courses
     * Security: PUBLIC - Anyone can see course list
     */
    public Function<Request, Response> getAllCourses() {
        return request -> {
            try {
                logger.info("🔧 AI Function Called: getAllCourses");
                PageResponse<CourseDTO> pageResponse = courseClient.getAllCourses(0, 100);
                List<CourseDTO> courses = pageResponse.getContent();
                
                StringBuilder result = new StringBuilder();
                result.append("Available Courses:\n\n");
                
                for (CourseDTO course : courses) {
                    result.append("- ").append(course.getTitle()).append("\n");
                    if (course.getDescription() != null) {
                        String desc = course.getDescription().length() > 100 
                            ? course.getDescription().substring(0, 100) + "..." 
                            : course.getDescription();
                        result.append("  Description: ").append(desc).append("\n");
                    }
                    if (course.getLevel() != null) {
                        result.append("  Level: ").append(course.getLevel()).append("\n");
                    }
                    result.append("\n");
                }
                
                return new Response(result.toString());
            } catch (Exception e) {
                logger.error("❌ Error in getAllCourses: {}", e.getMessage());
                return new Response("Error fetching courses: " + e.getMessage());
            }
        };
    }

    /**
     * Function: Get learner's enrollments with real progress
     * Security: PRIVATE - Only learner's own data
     */
    public Function<LearnerRequest, Response> getLearnerEnrollments() {
        return request -> {
            try {
                logger.info("🔧 AI Function Called: getLearnerEnrollments for learner: {}", request.learnerId);
                
                if (request.learnerId == null || request.learnerId.trim().isEmpty()) {
                    return new Response("Error: Learner ID is required");
                }
                
                // Get enrollments
                List<Map<String, Object>> enrollments = enrollmentClient.getEnrollmentsByLearnerId(request.learnerId);
                
                if (enrollments.isEmpty()) {
                    return new Response("No enrollments found for this learner.");
                }
                
                // Get progress data from MS-COURSE
                List<Map<String, Object>> progressList = null;
                try {
                    progressList = courseClient.getLearnerProgress(request.learnerId);
                } catch (Exception e) {
                    logger.warn("Could not fetch progress data: {}", e.getMessage());
                }
                
                // Create a map of courseId -> progress for quick lookup
                Map<Long, Map<String, Object>> progressMap = new HashMap<>();
                if (progressList != null) {
                    for (Map<String, Object> progress : progressList) {
                        Object courseIdObj = progress.get("courseId");
                        if (courseIdObj instanceof Number) {
                            Long courseId = ((Number) courseIdObj).longValue();
                            progressMap.put(courseId, progress);
                        }
                    }
                }
                
                StringBuilder result = new StringBuilder();
                result.append("Learner's Enrolled Courses:\n\n");
                
                for (Map<String, Object> enrollment : enrollments) {
                    Long courseId = getCourseIdFromEnrollment(enrollment);
                    String status = (String) enrollment.get("status");
                    
                    if (courseId != null) {
                        try {
                            String courseTitle = courseClient.getCourseTitle(courseId);
                            result.append("- ").append(courseTitle).append("\n");
                            result.append("  courseId: ").append(courseId).append("\n");
                            result.append("  Status: ").append(status).append("\n");
                            
                            // Get real progress from MS-COURSE
                            Map<String, Object> progress = progressMap.get(courseId);
                            if (progress != null) {
                                Object progressPercentObj = progress.get("progressPercent");
                                Object completedLessonsObj = progress.get("completedLessons");
                                Object totalLessonsObj = progress.get("totalLessons");
                                
                                int progressPercent = progressPercentObj instanceof Number ? 
                                    ((Number) progressPercentObj).intValue() : 0;
                                int completedLessons = completedLessonsObj instanceof Number ? 
                                    ((Number) completedLessonsObj).intValue() : 0;
                                int totalLessons = totalLessonsObj instanceof Number ? 
                                    ((Number) totalLessonsObj).intValue() : 0;
                                
                                result.append("  Progress: ").append(progressPercent).append("%");
                                result.append(" (").append(completedLessons).append("/").append(totalLessons).append(" lessons)\n");
                            } else {
                                result.append("  Progress: 0% (not started)\n");
                            }
                            
                            result.append("\n");
                        } catch (Exception e) {
                            logger.warn("Could not fetch course title for ID: {}", courseId);
                        }
                    }
                }
                
                return new Response(result.toString());
            } catch (Exception e) {
                logger.error("❌ Error in getLearnerEnrollments: {}", e.getMessage());
                return new Response("Error fetching enrollments: " + e.getMessage());
            }
        };
    }

    /**
     * Function: Get course chapters
     * Security: PRIVATE - Only if learner is enrolled
     */
    public Function<CourseRequest, Response> getCourseChapters() {
        return request -> {
            try {
                logger.info("🔧 AI Function Called: getCourseChapters for course: {}, learner: {}", 
                    request.courseId, request.learnerId);
                
                // Check if learner is enrolled
                if (!isLearnerEnrolledInCourse(request.learnerId, request.courseId)) {
                    return new Response("Error: You must be enrolled in this course to access its content.");
                }
                
                List<ChapterDTO> chapters = courseClient.getChaptersByCourse(request.courseId);
                
                if (chapters.isEmpty()) {
                    return new Response("No chapters found for this course.");
                }
                
                StringBuilder result = new StringBuilder();
                result.append("Course Chapters:\n\n");
                
                int chapterNum = 1;
                for (ChapterDTO chapter : chapters) {
                    result.append(chapterNum++).append(". ").append(chapter.getTitle()).append("\n");
                    if (chapter.getDescription() != null && !chapter.getDescription().isEmpty()) {
                        result.append("   ").append(chapter.getDescription()).append("\n");
                    }
                    result.append("\n");
                }
                
                return new Response(result.toString());
            } catch (Exception e) {
                logger.error("❌ Error in getCourseChapters: {}", e.getMessage());
                return new Response("Error fetching chapters: " + e.getMessage());
            }
        };
    }

    /**
     * Function: Get chapter content
     * Security: PRIVATE - Only if learner is enrolled
     */
    public Function<ChapterRequest, Response> getChapterContent() {
        return request -> {
            try {
                logger.info("🔧 AI Function Called: getChapterContent for course: {}, chapter: {}, learner: {}", 
                    request.courseId, request.chapterId, request.learnerId);
                
                // Check if learner is enrolled
                if (!isLearnerEnrolledInCourse(request.learnerId, request.courseId)) {
                    return new Response("Error: You must be enrolled in this course to access its content.");
                }
                
                ChapterDTO chapter = courseClient.getChapterWithContent(request.courseId, request.chapterId);
                
                StringBuilder result = new StringBuilder();
                result.append("Chapter: ").append(chapter.getTitle()).append("\n\n");
                
                if (chapter.getDescription() != null && !chapter.getDescription().isEmpty()) {
                    result.append("Description: ").append(chapter.getDescription()).append("\n\n");
                }
                
                if (chapter.getContentBlocks() != null && !chapter.getContentBlocks().isEmpty()) {
                    result.append("Content:\n\n");
                    
                    for (ContentBlockDTO block : chapter.getContentBlocks()) {
                        result.append("▶️ ").append(block.getTitle()).append("\n");
                        result.append("   Type: ").append(block.getType()).append("\n");
                        
                        if (block.getData() != null && !block.getData().isEmpty()) {
                            String content = extractContentFromBlock(block);
                            result.append("   ").append(content).append("\n");
                        }
                        result.append("\n");
                    }
                }
                
                return new Response(result.toString());
            } catch (Exception e) {
                logger.error("❌ Error in getChapterContent: {}", e.getMessage());
                return new Response("Error fetching chapter content: " + e.getMessage());
            }
        };
    }

    /**
     * Function: Get platform statistics
     * Security: PUBLIC - General statistics
     */
    public Function<Request, Response> getPlatformStatistics() {
        return request -> {
            try {
                logger.info("🔧 AI Function Called: getPlatformStatistics");
                
                // Get all enrollments
                List<Map<String, Object>> allEnrollments = enrollmentClient.getAllEnrollments();
                
                // Get all courses
                PageResponse<CourseDTO> pageResponse = courseClient.getAllCourses(0, 100);
                List<CourseDTO> courses = pageResponse.getContent();
                
                // Calculate statistics
                int totalEnrollments = allEnrollments.size();
                int totalCourses = courses.size();
                
                // Count unique learners
                Set<String> uniqueLearners = new HashSet<>();
                int completedEnrollments = 0;
                
                for (Map<String, Object> enrollment : allEnrollments) {
                    String learnerId = (String) enrollment.get("learnerId");
                    if (learnerId != null) {
                        uniqueLearners.add(learnerId);
                    }
                    
                    String status = (String) enrollment.get("status");
                    if ("COMPLETED".equals(status)) {
                        completedEnrollments++;
                    }
                }
                
                StringBuilder result = new StringBuilder();
                result.append("Platform Statistics:\n\n");
                result.append("📚 Total Courses: ").append(totalCourses).append("\n");
                result.append("👥 Total Learners: ").append(uniqueLearners.size()).append("\n");
                result.append("📝 Total Enrollments: ").append(totalEnrollments).append("\n");
                result.append("✅ Completed Enrollments: ").append(completedEnrollments).append("\n");
                
                if (totalEnrollments > 0) {
                    double completionRate = (completedEnrollments * 100.0) / totalEnrollments;
                    result.append("📊 Completion Rate: ").append(String.format("%.1f%%", completionRate)).append("\n");
                }
                
                return new Response(result.toString());
            } catch (Exception e) {
                logger.error("❌ Error in getPlatformStatistics: {}", e.getMessage());
                return new Response("Error fetching statistics: " + e.getMessage());
            }
        };
    }

    /**
     * Function: Search courses by keyword
     * Security: PUBLIC - Anyone can search
     */
    public Function<SearchRequest, Response> searchCourses() {
        return request -> {
            try {
                logger.info("🔧 AI Function Called: searchCourses with keyword: {}", request.keyword);
                
                PageResponse<CourseDTO> pageResponse = courseClient.getAllCourses(0, 100);
                List<CourseDTO> allCourses = pageResponse.getContent();
                
                String keyword = request.keyword.toLowerCase();
                List<CourseDTO> matchedCourses = new ArrayList<>();
                
                for (CourseDTO course : allCourses) {
                    if (course.getTitle().toLowerCase().contains(keyword) ||
                        (course.getDescription() != null && course.getDescription().toLowerCase().contains(keyword))) {
                        matchedCourses.add(course);
                    }
                }
                
                if (matchedCourses.isEmpty()) {
                    return new Response("No courses found matching: " + request.keyword);
                }
                
                StringBuilder result = new StringBuilder();
                result.append("Courses matching '").append(request.keyword).append("':\n\n");
                
                for (CourseDTO course : matchedCourses) {
                    result.append("- ").append(course.getTitle()).append("\n");
                    if (course.getDescription() != null) {
                        String desc = course.getDescription().length() > 100 
                            ? course.getDescription().substring(0, 100) + "..." 
                            : course.getDescription();
                        result.append("  ").append(desc).append("\n");
                    }
                    result.append("\n");
                }
                
                return new Response(result.toString());
            } catch (Exception e) {
                logger.error("❌ Error in searchCourses: {}", e.getMessage());
                return new Response("Error searching courses: " + e.getMessage());
            }
        };
    }

    // Helper methods

    private boolean isLearnerEnrolledInCourse(String learnerId, Long courseId) {
        try {
            List<Map<String, Object>> enrollments = enrollmentClient.getEnrollmentsByLearnerId(learnerId);
            
            for (Map<String, Object> enrollment : enrollments) {
                Long enrolledCourseId = getCourseIdFromEnrollment(enrollment);
                String status = (String) enrollment.get("status");
                
                if (courseId.equals(enrolledCourseId) && "ACTIVE".equals(status)) {
                    return true;
                }
            }
            
            return false;
        } catch (Exception e) {
            logger.error("Error checking enrollment: {}", e.getMessage());
            return false;
        }
    }

    private Long getCourseIdFromEnrollment(Map<String, Object> enrollment) {
        Object courseIdObj = enrollment.get("courseId");
        if (courseIdObj == null) {
            courseIdObj = enrollment.get("CourseId");
        }
        
        if (courseIdObj instanceof Number) {
            return ((Number) courseIdObj).longValue();
        }
        
        return null;
    }

    private String extractContentFromBlock(ContentBlockDTO block) {
        String data = block.getData();
        String type = block.getType();
        
        if ("TEXT".equals(type)) {
            // Extract text content
            if (data.contains("\"text\"")) {
                int start = data.indexOf("\"text\"") + 8;
                int end = data.indexOf("\"", start + 1);
                if (end > start) {
                    return data.substring(start, end);
                }
            }
            return "Text content available";
        } else if ("IMAGE".equals(type)) {
            return "Image content (visual)";
        } else if ("VIDEO".equals(type)) {
            return "Video content (visual)";
        } else if ("CODE".equals(type)) {
            if (data.contains("\"code\"")) {
                int start = data.indexOf("\"code\"") + 8;
                int end = data.indexOf("\"", start + 1);
                if (end > start) {
                    String code = data.substring(start, end);
                    return "Code: " + (code.length() > 100 ? code.substring(0, 100) + "..." : code);
                }
            }
            return "Code snippet available";
        }
        
        return "Content available";
    }

    // Request/Response DTOs for function calling

    public static class Request {
        // Empty request for functions that don't need parameters
    }

    public static class Response {
        public String result;
        
        public Response(String result) {
            this.result = result;
        }
    }

    public static class LearnerRequest {
        public String learnerId;
    }

    public static class CourseRequest {
        public String learnerId;
        public Long courseId;
    }

    public static class ChapterRequest {
        public String learnerId;
        public Long courseId;
        public Long chapterId;
    }

    public static class SearchRequest {
        public String keyword;
    }
}
