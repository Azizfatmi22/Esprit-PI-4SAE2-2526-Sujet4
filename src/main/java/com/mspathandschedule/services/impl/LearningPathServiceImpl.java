package com.mspathandschedule.services.impl;

import com.mspathandschedule.clients.CourseManagementFeignClient;
import com.mspathandschedule.clients.SessionManagementFeignClient;
import com.mspathandschedule.entities.LearningPath;
import com.mspathandschedule.repositories.LearningPathRepository;
import com.mspathandschedule.services.interfaces.LearningPathService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class LearningPathServiceImpl implements LearningPathService {

    private static final Logger log = LoggerFactory.getLogger(LearningPathServiceImpl.class);

    private final LearningPathRepository repository;
    private final SessionManagementFeignClient sessionClient;
    private final CourseManagementFeignClient courseClient;

    public LearningPathServiceImpl(LearningPathRepository repository,
                                   SessionManagementFeignClient sessionClient,
                                   CourseManagementFeignClient courseClient) {
        this.repository = repository;
        this.sessionClient = sessionClient;
        this.courseClient = courseClient;
    }

    // ---------------- CRUD ----------------
    @Override
    public LearningPath createLearningPath(LearningPath lp) {
        // Validate sessions before saving
        if (lp.getSessionIds() != null && !lp.getSessionIds().isEmpty()) {
            int totalHours = 0;
            for (Long sessionId : lp.getSessionIds()) {
                Map<String, Object> session = getSessionWithPlanning(sessionId);
                if (session != null) {
                    // Extract planning from session (session contains planning via OneToOne)
                    Map<String, Object> planning = (Map<String, Object>) session.get("planning");
                    if (planning != null && planning.get("totalHours") != null) {
                        totalHours += (Integer) planning.get("totalHours");
                    }
                }
            }
            lp.setTotalHours(totalHours);
        }
        return repository.save(lp);
    }

    @Override
    public LearningPath updateLearningPath(Long id, LearningPath lp) {
        LearningPath existing = getLearningPath(id);

        existing.setTitle(lp.getTitle());
        existing.setDescription(lp.getDescription());
        existing.setLevel(lp.getLevel());
        existing.setStatus(lp.getStatus());
        existing.setObjectives(lp.getObjectives());

        if (lp.getSessionIds() != null) {
            existing.setSessionIds(lp.getSessionIds());
            // Recalculate total hours
            int totalHours = 0;
            for (Long sessionId : lp.getSessionIds()) {
                Map<String, Object> session = getSessionWithPlanning(sessionId);
                if (session != null) {
                    Map<String, Object> planning = (Map<String, Object>) session.get("planning");
                    if (planning != null && planning.get("totalHours") != null) {
                        totalHours += (Integer) planning.get("totalHours");
                    }
                }
            }
            existing.setTotalHours(totalHours);
        }

        return repository.save(existing);
    }

    @Override
    public LearningPath getLearningPath(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("LearningPath not found"));
    }

    @Override
    public List<LearningPath> getAllLearningPaths() {
        return repository.findAll();
    }

    @Override
    public void deleteLearningPath(Long id) {
        repository.deleteById(id);
    }

    // ---------------- Session Management ----------------
    @Override
    public LearningPath addSessionToPath(Long pathId, Long sessionId) {
        LearningPath lp = getLearningPath(pathId);

        if (!lp.getSessionIds().contains(sessionId)) {
            // Validate session exists
            Map<String, Object> session = getSessionWithPlanning(sessionId);
            if (session == null) {
                throw new RuntimeException("Session " + sessionId + " does not exist");
            }
            lp.getSessionIds().add(sessionId);

            // Update total hours
            Map<String, Object> planning = (Map<String, Object>) session.get("planning");
            if (planning != null && planning.get("totalHours") != null) {
                int currentTotal = lp.getTotalHours() != null ? lp.getTotalHours() : 0;
                lp.setTotalHours(currentTotal + (Integer) planning.get("totalHours"));
            }
        }

        return repository.save(lp);
    }

    @Override
    public LearningPath removeSessionFromPath(Long pathId, Long sessionId) {
        LearningPath lp = getLearningPath(pathId);

        if (lp.getSessionIds().contains(sessionId)) {
            // Subtract hours before removing
            Map<String, Object> session = getSessionWithPlanning(sessionId);
            if (session != null) {
                Map<String, Object> planning = (Map<String, Object>) session.get("planning");
                if (planning != null && planning.get("totalHours") != null) {
                    int currentTotal = lp.getTotalHours() != null ? lp.getTotalHours() : 0;
                    lp.setTotalHours(currentTotal - (Integer) planning.get("totalHours"));
                }
            }
            lp.getSessionIds().remove(sessionId);
        }

        return repository.save(lp);
    }

    // ---------------- Helper Methods ----------------
    private Map<String, Object> getSessionWithPlanning(Long sessionId) {
        try {
            // Session contains planning via @OneToOne relationship
            return sessionClient.getSessionById(sessionId);
        } catch (Exception e) {
            log.error("Error fetching session {}: {}", sessionId, e.getMessage());
            return null;
        }
    }

    // ---------------- Course Filtering Functions ----------------

    // 5. Filter Courses by Level

    public List<Map<String, Object>> filterCoursesByLevel(String level) {
        log.info("Filtering courses by level: {}", level);

        if (level == null || level.isEmpty()) {
            log.warn("Level parameter is empty");
            return new ArrayList<>();
        }

        try {
            String levelParam = level.toUpperCase();
            List<Map<String, Object>> courses = courseClient.getCoursesByLevel(levelParam);
            log.info("Found {} courses with level: {}", courses.size(), level);
            return courses;
        } catch (Exception e) {
            log.error("Error fetching courses by level {}: {}", level, e.getMessage());
            return new ArrayList<>();
        }
    }

    // 6. Filter Courses by Description (using search endpoint)
    
    public List<Map<String, Object>> filterCoursesByDescription(String keyword) {
        log.info("Filtering courses by description keyword: {}", keyword);

        if (keyword == null || keyword.isEmpty()) {
            log.warn("Keyword parameter is empty");
            return new ArrayList<>();
        }

        try {
            List<Map<String, Object>> courses = courseClient.searchCourses(keyword);
            log.info("Found {} courses containing '{}'", courses.size(), keyword);
            return courses;
        } catch (Exception e) {
            log.error("Error searching courses by keyword '{}': {}", keyword, e.getMessage());
            return new ArrayList<>();
        }
    }

    // ---------------- Advanced Analytics Functions ----------------

    // 1. Calculate Path Complexity
    public Map<String, Object> calculatePathComplexity(Long pathId) {
        LearningPath lp = getLearningPath(pathId);
        Map<String, Object> result = new HashMap<>();

        List<Map<String, Object>> sessions = new ArrayList<>();
        for (Long sessionId : lp.getSessionIds()) {
            Map<String, Object> session = getSessionWithPlanning(sessionId);
            if (session != null) sessions.add(session);
        }

        int totalSessions = sessions.size();
        int totalHours = 0;
        Map<String, Integer> difficultyCount = new HashMap<>();
        difficultyCount.put("BEGINNER", 0);
        difficultyCount.put("INTERMEDIATE", 0);
        difficultyCount.put("ADVANCED", 0);

        for (Map<String, Object> session : sessions) {
            Map<String, Object> planning = (Map<String, Object>) session.get("planning");
            if (planning != null && planning.get("totalHours") != null) {
                totalHours += (Integer) planning.get("totalHours");
            }
            String status = (String) session.get("status");
            if (status != null) {
                // You can add logic for status if needed
            }
        }

        // For difficulty, we can infer from session hours or other metrics
        // Since Session entity doesn't have level, we'll use hours as proxy
        double avgHoursPerSession = totalSessions > 0 ? (double) totalHours / totalSessions : 0;

        int complexityScore = 0;
        if (avgHoursPerSession > 8) complexityScore += 30;
        else if (avgHoursPerSession > 6) complexityScore += 20;
        else if (avgHoursPerSession > 4) complexityScore += 10;

        if (totalSessions > 15) complexityScore += 30;
        else if (totalSessions > 10) complexityScore += 20;
        else if (totalSessions > 5) complexityScore += 10;

        // For advanced count, check sessions with > 8 hours
        int advancedCount = 0;
        for (Map<String, Object> session : sessions) {
            Map<String, Object> planning = (Map<String, Object>) session.get("planning");
            if (planning != null && planning.get("totalHours") != null) {
                if ((Integer) planning.get("totalHours") > 8) advancedCount++;
            }
        }

        if (advancedCount > totalSessions / 2) complexityScore += 40;
        else if (advancedCount > 0) complexityScore += 20;

        String complexityLevel;
        if (complexityScore >= 70) complexityLevel = "ÉLEVÉE";
        else if (complexityScore >= 40) complexityLevel = "MOYENNE";
        else complexityLevel = "FAIBLE";

        result.put("complexityScore", complexityScore);
        result.put("complexityLevel", complexityLevel);
        result.put("totalSessions", totalSessions);
        result.put("totalHours", totalHours);
        result.put("avgHoursPerSession", Math.round(avgHoursPerSession * 10) / 10.0);

        return result;
    }

    // 2. Predict Completion Rate
    public Map<String, Object> predictCompletionRate(Long pathId) {
        LearningPath lp = getLearningPath(pathId);
        Map<String, Object> prediction = new HashMap<>();

        List<Map<String, Object>> sessions = new ArrayList<>();
        for (Long sessionId : lp.getSessionIds()) {
            Map<String, Object> session = getSessionWithPlanning(sessionId);
            if (session != null) sessions.add(session);
        }

        int totalHours = 0;
        int totalSessions = sessions.size();
        int advancedCount = 0;

        for (Map<String, Object> session : sessions) {
            Map<String, Object> planning = (Map<String, Object>) session.get("planning");
            if (planning != null && planning.get("totalHours") != null) {
                int hours = (Integer) planning.get("totalHours");
                totalHours += hours;
                if (hours > 8) advancedCount++;
            }
        }

        double baseRate = 75.0;

        if (totalHours > 60) baseRate -= 20;
        else if (totalHours > 40) baseRate -= 10;
        else if (totalHours < 20) baseRate += 5;

        if (totalSessions > 15) baseRate -= 15;
        else if (totalSessions > 10) baseRate -= 8;

        if (advancedCount > totalSessions / 2) baseRate -= 15;
        else if (advancedCount > 0) baseRate -= 5;

        double avgHoursPerSession = totalSessions > 0 ? (double) totalHours / totalSessions : 0;
        if (avgHoursPerSession > 8) baseRate -= 10;

        baseRate = Math.max(0, Math.min(100, baseRate));

        String riskLevel;
        if (baseRate < 50) riskLevel = "ÉLEVÉ";
        else if (baseRate < 70) riskLevel = "MOYEN";
        else riskLevel = "FAIBLE";

        prediction.put("predictedCompletionRate", Math.round(baseRate));
        prediction.put("riskLevel", riskLevel);
        prediction.put("totalHours", totalHours);
        prediction.put("totalSessions", totalSessions);

        return prediction;
    }

    // 3. Generate Learning Summary
    public String generateLearningSummary(Long pathId) {
        LearningPath lp = getLearningPath(pathId);
        StringBuilder summary = new StringBuilder();

        List<Map<String, Object>> sessions = new ArrayList<>();
        for (Long sessionId : lp.getSessionIds()) {
            Map<String, Object> session = getSessionWithPlanning(sessionId);
            if (session != null) sessions.add(session);
        }

        int totalHours = 0;
        Set<String> topics = new HashSet<>();

        for (Map<String, Object> session : sessions) {
            Map<String, Object> planning = (Map<String, Object>) session.get("planning");
            if (planning != null && planning.get("totalHours") != null) {
                totalHours += (Integer) planning.get("totalHours");
            }
            String title = (String) session.get("title");
            if (title != null) {
                String[] words = title.split(" ");
                for (String word : words) {
                    if (word.length() > 4) {
                        topics.add(word);
                    }
                }
            }
        }

        String topicsStr = String.join(", ", topics.stream().limit(5).collect(Collectors.toList()));

        summary.append("📚 **").append(lp.getTitle()).append("**\n\n");
        summary.append(lp.getDescription()).append("\n\n");
        summary.append("🎯 **Objectifs:**\n");
        summary.append(lp.getObjectives()).append("\n\n");
        summary.append("📊 **Chiffres clés:**\n");
        summary.append("- ").append(sessions.size()).append(" sessions\n");
        summary.append("- ").append(totalHours).append(" heures\n");
        summary.append("- Niveau: ").append(lp.getLevel()).append("\n\n");

        if (!topicsStr.isEmpty()) {
            summary.append("💡 **Thèmes abordés:**\n");
            summary.append(topicsStr).append("\n\n");
        }

        return summary.toString();
    }

    // 4. Get Optimal Learning Order
    public List<Map<String, Object>> getOptimalLearningOrder(Long pathId) {
        LearningPath lp = getLearningPath(pathId);
        List<Map<String, Object>> sessions = new ArrayList<>();

        for (Long sessionId : lp.getSessionIds()) {
            Map<String, Object> session = getSessionWithPlanning(sessionId);
            if (session != null) sessions.add(session);
        }

        // Sort by total hours (easier sessions first)
        sessions.sort((s1, s2) -> {
            Map<String, Object> p1 = (Map<String, Object>) s1.get("planning");
            Map<String, Object> p2 = (Map<String, Object>) s2.get("planning");
            int h1 = p1 != null && p1.get("totalHours") != null ? (Integer) p1.get("totalHours") : 0;
            int h2 = p2 != null && p2.get("totalHours") != null ? (Integer) p2.get("totalHours") : 0;
            return Integer.compare(h1, h2);
        });

        return sessions;
    }
}