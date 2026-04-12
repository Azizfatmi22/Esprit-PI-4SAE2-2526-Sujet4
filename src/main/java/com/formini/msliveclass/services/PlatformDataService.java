package com.formini.msliveclass.services;

import com.formini.msliveclass.clients.CourseClient;
import com.formini.msliveclass.clients.EnrollmentClient;
import com.formini.msliveclass.dto.CourseDTO;
import com.formini.msliveclass.dto.PageResponse;
import com.formini.msliveclass.entities.LiveSession;
import com.formini.msliveclass.repositories.LiveSessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class PlatformDataService {

    private final CourseClient courseClient;
    private final EnrollmentClient enrollmentClient;
    private final LiveSessionRepository liveSessionRepository;

    @Autowired
    public PlatformDataService(CourseClient courseClient,
                              EnrollmentClient enrollmentClient,
                              LiveSessionRepository liveSessionRepository) {
        this.courseClient = courseClient;
        this.enrollmentClient = enrollmentClient;
        this.liveSessionRepository = liveSessionRepository;
    }

    /**
     * Get comprehensive platform statistics
     */
    public String getPlatformStatistics() {
        StringBuilder stats = new StringBuilder();
        
        try {
            // Get all enrollments
            List<Map<String, Object>> enrollments = enrollmentClient.getAllEnrollments();
            
            // Calculate statistics
            int totalEnrollments = enrollments.size();
            int activeEnrollments = (int) enrollments.stream()
                    .filter(e -> "ACTIVE".equals(e.get("status")))
                    .count();
            int completedEnrollments = (int) enrollments.stream()
                    .filter(e -> "COMPLETED".equals(e.get("status")))
                    .count();
            
            // Get unique learners
            Set<String> uniqueLearners = enrollments.stream()
                    .map(e -> (String) e.get("learnerId"))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            
            // Get live sessions
            List<LiveSession> allSessions = liveSessionRepository.findAll();
            long activeSessions = allSessions.stream()
                    .filter(s -> Boolean.TRUE.equals(s.getIsActive()))
                    .count();
            
            stats.append("📊 Platform Statistics:\n\n");
            stats.append("👥 Total Learners: ").append(uniqueLearners.size()).append("\n");
            stats.append("📚 Total Enrollments: ").append(totalEnrollments).append("\n");
            stats.append("✅ Active Enrollments: ").append(activeEnrollments).append("\n");
            stats.append("🎓 Completed Enrollments: ").append(completedEnrollments).append("\n");
            stats.append("🎥 Active Live Sessions: ").append(activeSessions).append("\n");
            stats.append("📺 Total Live Sessions: ").append(allSessions.size()).append("\n");
            
            // Calculate completion rate
            if (totalEnrollments > 0) {
                double completionRate = (completedEnrollments * 100.0) / totalEnrollments;
                stats.append("📈 Completion Rate: ").append(String.format("%.1f%%", completionRate)).append("\n");
            }
            
        } catch (Exception e) {
            stats.append("Unable to fetch complete statistics at the moment.\n");
        }
        
        return stats.toString();
    }

    /**
     * Get top performing learners based on completion and progress
     */
    public String getTopLearners(int limit) {
        StringBuilder result = new StringBuilder();
        
        try {
            List<Map<String, Object>> enrollments = enrollmentClient.getAllEnrollments();
            
            // Group by learner and calculate their stats
            Map<String, LearnerStats> learnerStatsMap = new HashMap<>();
            
            for (Map<String, Object> enrollment : enrollments) {
                String learnerId = (String) enrollment.get("learnerId");
                if (learnerId == null) continue;
                
                LearnerStats stats = learnerStatsMap.getOrDefault(learnerId, new LearnerStats(learnerId));
                
                stats.totalEnrollments++;
                
                if ("COMPLETED".equals(enrollment.get("status"))) {
                    stats.completedCourses++;
                }
                
                Object progressObj = enrollment.get("progress");
                if (progressObj != null) {
                    double progress = progressObj instanceof Number ? 
                            ((Number) progressObj).doubleValue() : 0.0;
                    stats.totalProgress += progress;
                }
                
                learnerStatsMap.put(learnerId, stats);
            }
            
            // Calculate average progress and sort
            List<LearnerStats> topLearners = learnerStatsMap.values().stream()
                    .peek(stats -> stats.avgProgress = stats.totalProgress / stats.totalEnrollments)
                    .sorted((a, b) -> {
                        // Sort by completed courses first, then by average progress
                        int completedCompare = Integer.compare(b.completedCourses, a.completedCourses);
                        if (completedCompare != 0) return completedCompare;
                        return Double.compare(b.avgProgress, a.avgProgress);
                    })
                    .limit(limit)
                    .collect(Collectors.toList());
            
            result.append("🏆 Top ").append(limit).append(" Learners:\n\n");
            
            int rank = 1;
            for (LearnerStats stats : topLearners) {
                String medal = rank == 1 ? "🥇" : rank == 2 ? "🥈" : rank == 3 ? "🥉" : "🎖️";
                result.append(medal).append(" #").append(rank).append(" - Learner ID: ").append(stats.learnerId).append("\n");
                result.append("   📚 Enrolled: ").append(stats.totalEnrollments).append(" courses\n");
                result.append("   ✅ Completed: ").append(stats.completedCourses).append(" courses\n");
                result.append("   📊 Avg Progress: ").append(String.format("%.1f%%", stats.avgProgress)).append("\n\n");
                rank++;
            }
            
        } catch (Exception e) {
            result.append("Unable to fetch top learners at the moment.\n");
        }
        
        return result.toString();
    }

    /**
     * Get detailed course information
     */
    public String getCourseDetails(Long courseId) {
        StringBuilder details = new StringBuilder();
        
        try {
            CourseDTO course = courseClient.getCourseById(courseId);
            
            details.append("📚 Course Details:\n\n");
            details.append("Title: ").append(course.getTitle()).append("\n");
            details.append("Description: ").append(course.getDescription()).append("\n");
            details.append("Level: ").append(course.getLevel()).append("\n");
            details.append("Price: $").append(course.getPrice()).append("\n");
            
            if (course.getDurationMinutes() != null) {
                int hours = course.getDurationMinutes() / 60;
                int minutes = course.getDurationMinutes() % 60;
                details.append("Duration: ").append(hours).append("h ").append(minutes).append("m\n");
            }
            
            details.append("Status: ").append(course.getStatus()).append("\n");
            details.append("Enrolled Students: ").append(course.getEnrolledStudents()).append("\n");
            
            if (course.getRating() != null) {
                details.append("Rating: ⭐ ").append(String.format("%.1f", course.getRating())).append("/5\n");
            }
            
        } catch (Exception e) {
            details.append("Unable to fetch course details for ID: ").append(courseId).append("\n");
        }
        
        return details.toString();
    }

    /**
     * Get all available courses summary
     */
    public String getAllCoursesSummary() {
        StringBuilder summary = new StringBuilder();
        
        try {
            PageResponse<CourseDTO> pageResponse = courseClient.getAllCourses(0, 100);
            List<CourseDTO> courses = pageResponse.getContent();
            
            summary.append("📚 Available Courses (").append(courses.size()).append(" total):\n\n");
            
            // Group by level
            Map<String, List<CourseDTO>> coursesByLevel = courses.stream()
                    .collect(Collectors.groupingBy(c -> c.getLevel() != null ? c.getLevel() : "Unknown"));
            
            for (Map.Entry<String, List<CourseDTO>> entry : coursesByLevel.entrySet()) {
                summary.append("📖 ").append(entry.getKey()).append(" Level (").append(entry.getValue().size()).append(" courses):\n");
                
                for (CourseDTO course : entry.getValue()) {
                    summary.append("  • ").append(course.getTitle());
                    if (course.getEnrolledStudents() != null && course.getEnrolledStudents() > 0) {
                        summary.append(" (").append(course.getEnrolledStudents()).append(" students)");
                    }
                    if (course.getRating() != null) {
                        summary.append(" ⭐").append(String.format("%.1f", course.getRating()));
                    }
                    summary.append("\n");
                }
                summary.append("\n");
            }
            
        } catch (Exception e) {
            summary.append("Unable to fetch courses at the moment.\n");
        }
        
        return summary.toString();
    }

    /**
     * Get learner's enrollment details
     */
    public String getLearnerEnrollments(String learnerId) {
        StringBuilder details = new StringBuilder();
        
        try {
            List<Map<String, Object>> enrollments = enrollmentClient.getEnrollmentsByLearnerId(learnerId);
            
            details.append("📚 Enrollments for Learner ").append(learnerId).append(":\n\n");
            
            if (enrollments.isEmpty()) {
                details.append("No enrollments found for this learner.\n");
                return details.toString();
            }
            
            for (Map<String, Object> enrollment : enrollments) {
                Long courseId = enrollment.get("courseId") instanceof Number ? 
                        ((Number) enrollment.get("courseId")).longValue() : null;
                
                if (courseId != null) {
                    try {
                        String courseTitle = courseClient.getCourseTitle(courseId);
                        details.append("📖 ").append(courseTitle).append("\n");
                    } catch (Exception e) {
                        details.append("📖 Course ID: ").append(courseId).append("\n");
                    }
                }
                
                details.append("   Status: ").append(enrollment.get("status")).append("\n");
                
                Object progressObj = enrollment.get("progress");
                if (progressObj != null) {
                    double progress = progressObj instanceof Number ? 
                            ((Number) progressObj).doubleValue() : 0.0;
                    details.append("   Progress: ").append(String.format("%.1f%%", progress)).append("\n");
                }
                
                details.append("\n");
            }
            
        } catch (Exception e) {
            details.append("Unable to fetch enrollments for learner ").append(learnerId).append("\n");
        }
        
        return details.toString();
    }

    /**
     * Get most popular courses
     */
    public String getMostPopularCourses(int limit) {
        StringBuilder result = new StringBuilder();
        
        try {
            PageResponse<CourseDTO> pageResponse = courseClient.getAllCourses(0, 100);
            List<CourseDTO> courses = pageResponse.getContent();
            
            List<CourseDTO> popularCourses = courses.stream()
                    .filter(c -> c.getEnrolledStudents() != null)
                    .sorted((a, b) -> Integer.compare(
                            b.getEnrolledStudents() != null ? b.getEnrolledStudents() : 0,
                            a.getEnrolledStudents() != null ? a.getEnrolledStudents() : 0
                    ))
                    .limit(limit)
                    .collect(Collectors.toList());
            
            result.append("🔥 Top ").append(limit).append(" Most Popular Courses:\n\n");
            
            int rank = 1;
            for (CourseDTO course : popularCourses) {
                result.append(rank).append(". ").append(course.getTitle()).append("\n");
                result.append("   👥 ").append(course.getEnrolledStudents()).append(" students enrolled\n");
                if (course.getRating() != null) {
                    result.append("   ⭐ ").append(String.format("%.1f", course.getRating())).append("/5\n");
                }
                result.append("   💰 $").append(course.getPrice()).append("\n\n");
                rank++;
            }
            
        } catch (Exception e) {
            result.append("Unable to fetch popular courses at the moment.\n");
        }
        
        return result.toString();
    }

    // Helper class for learner statistics
    private static class LearnerStats {
        String learnerId;
        int totalEnrollments = 0;
        int completedCourses = 0;
        double totalProgress = 0.0;
        double avgProgress = 0.0;
        
        LearnerStats(String learnerId) {
            this.learnerId = learnerId;
        }
    }
}
