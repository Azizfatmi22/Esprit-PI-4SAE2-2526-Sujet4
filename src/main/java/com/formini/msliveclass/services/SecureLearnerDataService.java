package com.formini.msliveclass.services;

import com.formini.msliveclass.clients.CourseClient;
import com.formini.msliveclass.clients.EnrollmentClient;
import com.formini.msliveclass.dto.ChapterDTO;
import com.formini.msliveclass.dto.ContentBlockDTO;
import com.formini.msliveclass.dto.CourseDTO;
import com.formini.msliveclass.dto.PageResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Secure service that provides ONLY the data a learner is allowed to access
 * Following strict privacy and security rules
 */
@Service
public class SecureLearnerDataService {

    private final CourseClient courseClient;
    private final EnrollmentClient enrollmentClient;

    @Autowired
    public SecureLearnerDataService(CourseClient courseClient, EnrollmentClient enrollmentClient) {
        this.courseClient = courseClient;
        this.enrollmentClient = enrollmentClient;
    }

    /**
     * Get all available courses - PUBLIC INFORMATION
     */
    public String getAllAvailableCourses() {
        try {
            PageResponse<CourseDTO> pageResponse = courseClient.getAllCourses(0, 100);
            List<CourseDTO> courses = pageResponse.getContent();
            
            if (courses == null || courses.isEmpty()) {
                return "No courses are currently available on the platform.";
            }

            StringBuilder result = new StringBuilder();
            result.append("📚 Available Courses on Formini Platform:\n\n");
            
            for (CourseDTO course : courses) {
                result.append("📖 ").append(course.getTitle()).append("\n");
                
                if (course.getDescription() != null && !course.getDescription().isEmpty()) {
                    String shortDesc = course.getDescription().length() > 100 
                        ? course.getDescription().substring(0, 100) + "..." 
                        : course.getDescription();
                    result.append("   ").append(shortDesc).append("\n");
                }
                
                if (course.getLevel() != null) {
                    result.append("   📊 Level: ").append(course.getLevel()).append("\n");
                }
                
                if (course.getDurationMinutes() != null) {
                    int hours = course.getDurationMinutes() / 60;
                    int minutes = course.getDurationMinutes() % 60;
                    result.append("   ⏱️ Duration: ").append(hours).append("h ").append(minutes).append("m\n");
                }
                
                result.append("\n");
            }
            
            result.append("💡 Enroll in any course to start learning!");
            
            return result.toString();
            
        } catch (Exception e) {
            return "I'm having trouble accessing the course catalog right now. Please try again in a moment.";
        }
    }

    /**
     * Get learner's own enrollment data - SAFE
     */
    public String getLearnerEnrollments(String learnerId) {
        if (learnerId == null || learnerId.trim().isEmpty()) {
            return "I need to know who you are to show your enrollments. Please make sure you're logged in.";
        }

        try {
            List<Map<String, Object>> enrollments = enrollmentClient.getEnrollmentsByLearnerId(learnerId);
            
            if (enrollments.isEmpty()) {
                return "You are not enrolled in any courses yet. Browse our course catalog to get started! 📚";
            }

            StringBuilder result = new StringBuilder();
            result.append("📚 Your Enrolled Courses:\n\n");
            
            int activeCount = 0;
            int completedCount = 0;
            
            for (Map<String, Object> enrollment : enrollments) {
                Long courseId = getCourseIdFromEnrollment(enrollment);
                String status = (String) enrollment.get("status");
                Object progressObj = enrollment.get("progress");
                double progress = progressObj instanceof Number ? ((Number) progressObj).doubleValue() : 0.0;
                
                if (courseId != null) {
                    try {
                        String courseTitle = courseClient.getCourseTitle(courseId);
                        result.append("📖 ").append(courseTitle).append("\n");
                        result.append("   Status: ").append(status).append("\n");
                        result.append("   Progress: ").append(String.format("%.1f%%", progress)).append("\n");
                        
                        if ("COMPLETED".equals(status)) {
                            result.append("   ✅ Completed!\n");
                            completedCount++;
                        } else if ("ACTIVE".equals(status)) {
                            activeCount++;
                        }
                        result.append("\n");
                    } catch (Exception e) {
                        // Course service unavailable, skip this course
                    }
                }
            }
            
            result.append("Summary: ").append(activeCount).append(" active, ")
                  .append(completedCount).append(" completed\n");
            
            return result.toString();
            
        } catch (Exception e) {
            return "I'm having trouble accessing your enrollment data right now. Please try again in a moment.";
        }
    }

    /**
     * Get course content ONLY if learner is enrolled - SECURE
     */
    public String getCourseContentForLearner(String learnerId, Long courseId) {
        if (!isLearnerEnrolledInCourse(learnerId, courseId)) {
            return "⚠️ You need to enroll in this course before I can help you with its content. " +
                   "Would you like to browse our course catalog?";
        }

        try {
            CourseDTO course = courseClient.getCourseWithChapters(courseId);
            
            StringBuilder content = new StringBuilder();
            content.append("📚 Course: ").append(course.getTitle()).append("\n\n");
            content.append("📝 Description:\n").append(course.getDescription()).append("\n\n");
            content.append("📊 Level: ").append(course.getLevel()).append("\n");
            
            if (course.getDurationMinutes() != null) {
                int hours = course.getDurationMinutes() / 60;
                int minutes = course.getDurationMinutes() % 60;
                content.append("⏱️ Duration: ").append(hours).append("h ").append(minutes).append("m\n");
            }
            
            // Include chapter information if available
            if (course.getChapters() != null && !course.getChapters().isEmpty()) {
                content.append("\n📑 Course Chapters:\n");
                int chapterNum = 1;
                for (Object chapterObj : course.getChapters()) {
                    // Since we don't have ChapterDTO in this service, we'll work with the object
                    content.append("  ").append(chapterNum++).append(". ");
                    content.append(chapterObj.toString()).append("\n");
                }
            }
            
            content.append("\nSince you're enrolled, feel free to ask me:\n");
            content.append("• Explain any lesson or concept\n");
            content.append("• Summarize chapters\n");
            content.append("• Give examples and exercises\n");
            content.append("• Clarify difficult topics\n");
            
            return content.toString();
            
        } catch (Exception e) {
            return "I couldn't retrieve the course details. Please try again.";
        }
    }

    /**
     * Get detailed chapter content for enrolled learner - SECURE
     */
    public String getChapterContentForLearner(String learnerId, Long courseId, Long chapterId) {
        if (!isLearnerEnrolledInCourse(learnerId, courseId)) {
            return "⚠️ You need to be enrolled in this course to access its content.";
        }

        try {
            ChapterDTO chapter = courseClient.getChapterWithContent(courseId, chapterId);
            StringBuilder content = new StringBuilder();
            content.append("📖 Chapter: ").append(chapter.getTitle()).append("\n\n");
            
            if (chapter.getDescription() != null && !chapter.getDescription().isEmpty()) {
                content.append("📝 Description:\n").append(chapter.getDescription()).append("\n\n");
            }
            
            if (chapter.getContentBlocks() != null && !chapter.getContentBlocks().isEmpty()) {
                content.append("📚 Content Blocks:\n\n");
                for (ContentBlockDTO block : chapter.getContentBlocks()) {
                    appendContentBlockInfo(content, block);
                    content.append("\n");
                }
            }
            return content.toString();
        } catch (Exception e) {
            return "I couldn't retrieve the chapter content. Please try again.";
        }
    }

    private void appendContentBlockInfo(StringBuilder content, ContentBlockDTO block) {
        content.append("▶️ ").append(block.getTitle()).append("\n");
        content.append("   Type: ").append(block.getType()).append("\n");

        if (block.getData() != null && !block.getData().isEmpty()) {
            String blockData = block.getData();
            String type = block.getType();

            if ("TEXT".equals(type)) {
                content.append("   Content: ").append(extractTextContent(blockData)).append("\n");
            } else if ("IMAGE".equals(type)) {
                content.append("   📷 Image: ").append(extractImageInfo(blockData)).append("\n");
            } else if ("VIDEO".equals(type)) {
                content.append("   🎥 Video: ").append(extractVideoInfo(blockData)).append("\n");
            } else if ("CODE".equals(type)) {
                content.append("   💻 Code: ").append(extractCodeContent(blockData)).append("\n");
            }
        }
    }

    /**
     * Extract text content from JSON data
     */
    private String extractTextContent(String jsonData) {
        try {
            // Simple extraction - in production, use proper JSON parsing
            if (jsonData.contains("\"text\"")) {
                int start = jsonData.indexOf("\"text\"") + 8;
                int end = jsonData.indexOf("\"", start + 1);
                if (end > start) {
                    String text = jsonData.substring(start, end);
                    return text.length() > 200 ? text.substring(0, 200) + "..." : text;
                }
            }
            return "Text content available";
        } catch (Exception e) {
            return "Text content";
        }
    }

    /**
     * Extract image information from JSON data
     */
    private String extractImageInfo(String jsonData) {
        try {
            if (jsonData.contains("\"url\"")) {
                int start = jsonData.indexOf("\"url\"") + 7;
                int end = jsonData.indexOf("\"", start + 1);
                if (end > start) {
                    return jsonData.substring(start, end);
                }
            }
            return "Image available";
        } catch (Exception e) {
            return "Image content";
        }
    }

    /**
     * Extract video information from JSON data
     */
    private String extractVideoInfo(String jsonData) {
        try {
            if (jsonData.contains("\"url\"")) {
                int start = jsonData.indexOf("\"url\"") + 7;
                int end = jsonData.indexOf("\"", start + 1);
                if (end > start) {
                    return jsonData.substring(start, end);
                }
            }
            return "Video available";
        } catch (Exception e) {
            return "Video content";
        }
    }

    /**
     * Extract code content from JSON data
     */
    private String extractCodeContent(String jsonData) {
        try {
            if (jsonData.contains("\"code\"")) {
                int start = jsonData.indexOf("\"code\"") + 8;
                int end = jsonData.indexOf("\"", start + 1);
                if (end > start) {
                    String code = jsonData.substring(start, end);
                    return code.length() > 150 ? code.substring(0, 150) + "..." : code;
                }
            }
            return "Code snippet available";
        } catch (Exception e) {
            return "Code content";
        }
    }

    /**
     * Get learner's progress in a specific course - SAFE
     */
    public String getLearnerProgressInCourse(String learnerId, Long courseId) {
        if (!isLearnerEnrolledInCourse(learnerId, courseId)) {
            return "You are not enrolled in this course.";
        }

        try {
            List<Map<String, Object>> enrollments = enrollmentClient.getEnrollmentsByLearnerId(learnerId);
            
            for (Map<String, Object> enrollment : enrollments) {
                Long enrolledCourseId = getCourseIdFromEnrollment(enrollment);
                
                if (courseId.equals(enrolledCourseId)) {
                    String courseTitle = courseClient.getCourseTitle(courseId);
                    String status = (String) enrollment.get("status");
                    Object progressObj = enrollment.get("progress");
                    double progress = progressObj instanceof Number ? ((Number) progressObj).doubleValue() : 0.0;
                    
                    StringBuilder result = new StringBuilder();
                    result.append("📊 Your Progress in ").append(courseTitle).append(":\n\n");
                    result.append("Status: ").append(status).append("\n");
                    result.append("Progress: ").append(String.format("%.1f%%", progress)).append("\n");
                    
                    if (progress < 25) {
                        result.append("\n💡 You're just getting started! Keep going!");
                    } else if (progress < 50) {
                        result.append("\n💪 Great progress! You're a quarter of the way there!");
                    } else if (progress < 75) {
                        result.append("\n🔥 Excellent! You're over halfway done!");
                    } else if (progress < 100) {
                        result.append("\n🎯 Almost there! Just a bit more to complete!");
                    } else {
                        result.append("\n🎉 Congratulations! You've completed this course!");
                    }
                    
                    return result.toString();
                }
            }
            
            return "I couldn't find your progress for this course.";
            
        } catch (Exception e) {
            return "I'm having trouble accessing your progress data right now.";
        }
    }

    /**
     * Recommend learning path based on learner's goal - SAFE
     */
    public String recommendLearningPath(String goal, String learnerId) {
        StringBuilder recommendation = new StringBuilder();
        recommendation.append("🎯 Learning Path for: ").append(goal).append("\n\n");
        
        // Get learner's current enrollments to personalize
        Set<String> enrolledCourses = new HashSet<>();
        try {
            List<Map<String, Object>> enrollments = enrollmentClient.getEnrollmentsByLearnerId(learnerId);
            for (Map<String, Object> enrollment : enrollments) {
                Long courseId = getCourseIdFromEnrollment(enrollment);
                if (courseId != null) {
                    try {
                        enrolledCourses.add(courseClient.getCourseTitle(courseId).toLowerCase());
                    } catch (Exception e) {
                        // Ignore
                    }
                }
            }
        } catch (Exception e) {
            // Continue without personalization
        }
        
        // Provide learning path based on goal
        String lowerGoal = goal.toLowerCase();
        
        if (lowerGoal.contains("full stack") || lowerGoal.contains("fullstack")) {
            recommendation.append("Recommended Path:\n");
            recommendation.append("1️⃣ Java Fundamentals\n");
            recommendation.append("2️⃣ Spring Boot Basics\n");
            recommendation.append("3️⃣ REST APIs & Web Services\n");
            recommendation.append("4️⃣ SQL & Database Design\n");
            recommendation.append("5️⃣ Frontend Framework (Angular/React)\n");
            recommendation.append("6️⃣ Authentication & Security (JWT)\n");
            recommendation.append("7️⃣ Docker & Deployment\n");
            recommendation.append("8️⃣ Full Stack Project\n");
            
        } else if (lowerGoal.contains("backend") || lowerGoal.contains("api")) {
            recommendation.append("Recommended Path:\n");
            recommendation.append("1️⃣ Java Programming\n");
            recommendation.append("2️⃣ Spring Boot Framework\n");
            recommendation.append("3️⃣ REST API Development\n");
            recommendation.append("4️⃣ Database & JPA\n");
            recommendation.append("5️⃣ Security & Authentication\n");
            recommendation.append("6️⃣ Microservices Architecture\n");
            recommendation.append("7️⃣ Testing & Best Practices\n");
            recommendation.append("8️⃣ Deployment & DevOps\n");
            
        } else if (lowerGoal.contains("frontend") || lowerGoal.contains("angular") || lowerGoal.contains("react")) {
            recommendation.append("Recommended Path:\n");
            recommendation.append("1️⃣ HTML, CSS & JavaScript\n");
            recommendation.append("2️⃣ TypeScript Basics\n");
            recommendation.append("3️⃣ Angular/React Framework\n");
            recommendation.append("4️⃣ Component Design\n");
            recommendation.append("5️⃣ State Management\n");
            recommendation.append("6️⃣ API Integration\n");
            recommendation.append("7️⃣ Testing & Debugging\n");
            recommendation.append("8️⃣ Performance Optimization\n");
            
        } else {
            recommendation.append("Based on your goal, I recommend:\n");
            recommendation.append("1️⃣ Start with fundamentals\n");
            recommendation.append("2️⃣ Build practical projects\n");
            recommendation.append("3️⃣ Learn industry best practices\n");
            recommendation.append("4️⃣ Master advanced concepts\n");
        }
        
        recommendation.append("\n💡 Browse our course catalog to find courses matching this path!");
        
        if (!enrolledCourses.isEmpty()) {
            recommendation.append("\n\n✅ You're already enrolled in: ");
            recommendation.append(String.join(", ", enrolledCourses));
        }
        
        return recommendation.toString();
    }

    /**
     * Get public leaderboard - ONLY PUBLIC INFO
     */
    public String getPublicLeaderboard(int limit) {
        try {
            List<Map<String, Object>> enrollments = enrollmentClient.getAllEnrollments();
            Map<String, LearnerPublicStats> statsMap = aggregateLearnerStats(enrollments);
            List<LearnerPublicStats> topLearners = rankTopLearners(statsMap, limit);
            return formatLeaderboardOutput(topLearners, limit);
        } catch (Exception e) {
            return "I couldn't retrieve the leaderboard right now. Please try again later.";
        }
    }

    private Map<String, LearnerPublicStats> aggregateLearnerStats(List<Map<String, Object>> enrollments) {
        Map<String, LearnerPublicStats> statsMap = new HashMap<>();
        for (Map<String, Object> enrollment : enrollments) {
            String learnerId = (String) enrollment.get("learnerId");
            if (learnerId == null) continue;

            LearnerPublicStats stats = statsMap.getOrDefault(learnerId, new LearnerPublicStats(learnerId));
            stats.totalEnrollments++;

            if ("COMPLETED".equals(enrollment.get("status"))) {
                stats.completedCourses++;
            }

            Object progressObj = enrollment.get("progress");
            if (progressObj instanceof Number) {
                stats.totalProgress += ((Number) progressObj).doubleValue();
            }
            statsMap.put(learnerId, stats);
        }
        return statsMap;
    }

    private List<LearnerPublicStats> rankTopLearners(Map<String, LearnerPublicStats> statsMap, int limit) {
        return statsMap.values().stream()
                .peek(stats -> {
                    stats.avgProgress = stats.totalProgress / stats.totalEnrollments;
                    stats.score = (stats.completedCourses * 100) + stats.avgProgress;
                })
                .sorted((a, b) -> Double.compare(b.score, a.score))
                .limit(limit)
                .collect(Collectors.toList());
    }

    private String formatLeaderboardOutput(List<LearnerPublicStats> topLearners, int limit) {
        StringBuilder result = new StringBuilder();
        result.append("🏆 Top ").append(limit).append(" Learners:\n\n");

        int rank = 1;
        for (LearnerPublicStats stats : topLearners) {
            String medal = getMedalForRank(rank);
            String displayName = "Learner " + stats.learnerId.substring(0, Math.min(8, stats.learnerId.length()));

            result.append(medal).append(" #").append(rank).append(" - ").append(displayName).append("\n");
            result.append("   📚 Enrolled: ").append(stats.totalEnrollments).append(" courses\n");
            result.append("   ✅ Completed: ").append(stats.completedCourses).append(" courses\n");
            result.append("   📊 Avg Progress: ").append(String.format("%.1f%%", stats.avgProgress)).append("\n\n");
            rank++;
        }
        return result.toString();
    }

    private String getMedalForRank(int rank) {
        if (rank == 1) return "🥇";
        if (rank == 2) return "🥈";
        if (rank == 3) return "🥉";
        return "🎖️";
    }

    /**
     * Check if learner is enrolled in a course - SECURITY CHECK
     */
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
            return false;
        }
    }

    /**
     * Safely extract course ID from enrollment map
     */
    private Long getCourseIdFromEnrollment(Map<String, Object> enrollment) {
        Object courseIdObj = enrollment.get("courseId");
        if (courseIdObj == null) {
            courseIdObj = enrollment.get("CourseId"); // Handle different casing
        }
        
        if (courseIdObj instanceof Number) {
            return ((Number) courseIdObj).longValue();
        }
        
        return null;
    }

    /**
     * Helper class for public learner statistics
     * NEVER exposes sensitive data
     */
    private static class LearnerPublicStats {
        String learnerId; // Only for grouping, never exposed directly
        int totalEnrollments = 0;
        int completedCourses = 0;
        double totalProgress = 0.0;
        double avgProgress = 0.0;
        double score = 0.0;
        
        LearnerPublicStats(String learnerId) {
            this.learnerId = learnerId;
        }
    }
}
