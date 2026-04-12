package com.formini.msliveclass.controllers;

import com.formini.msliveclass.dto.CourseDTO;
import com.formini.msliveclass.dto.ErrorResponse;
import com.formini.msliveclass.entities.LiveSession;
import com.formini.msliveclass.entities.Poll;
import com.formini.msliveclass.entities.ChatMessage;
import com.formini.msliveclass.repositories.LiveSessionRepository;
import com.formini.msliveclass.repositories.PollRepository;
import com.formini.msliveclass.repositories.PollVoteRepository;
import com.formini.msliveclass.repositories.ChatMessageRepository;
import com.formini.msliveclass.clients.CourseClient;
import com.formini.msliveclass.dto.LiveSessionResponse;
import com.formini.msliveclass.services.SessionAccessService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/livesession")
public class LiveSessionController {

    private static final Logger log = LoggerFactory.getLogger(LiveSessionController.class);

    private final LiveSessionRepository liveSessionRepository;
    private final CourseClient courseClient;
    private final SessionAccessService sessionAccessService;
    private final PollRepository pollRepository;
    private final PollVoteRepository pollVoteRepository;
    private final ChatMessageRepository chatMessageRepository;

    @Autowired
    public LiveSessionController(
            LiveSessionRepository liveSessionRepository,
            CourseClient courseClient,
            SessionAccessService sessionAccessService,
            PollRepository pollRepository,
            PollVoteRepository pollVoteRepository,
            ChatMessageRepository chatMessageRepository
    ) {
        this.liveSessionRepository = liveSessionRepository;
        this.courseClient = courseClient;
        this.sessionAccessService = sessionAccessService;
        this.pollRepository = pollRepository;
        this.pollVoteRepository = pollVoteRepository;
        this.chatMessageRepository = chatMessageRepository;
    }

    @PostMapping("/create")
    public ResponseEntity<?> createSession(
            @RequestBody LiveSession session,
            @RequestParam(value = "trainerId", required = true) String trainerId
    ) {
        if (session.getCourseId() == null) {
            return ResponseEntity.badRequest().body("Course ID is required.");
        }

        if (trainerId == null || trainerId.isBlank()) {
            return ResponseEntity.badRequest().body("trainerId query parameter is required.");
        }

        if (!sessionAccessService.isCourseTrainer(session.getCourseId(), trainerId)) {
            return ResponseEntity.status(403).body("Only the course trainer can create live sessions for this course.");
        }

        // Check if trainer has ANY active session across all courses
        List<LiveSession> allActiveSessions = liveSessionRepository.findAll().stream()
                .filter(s -> Boolean.TRUE.equals(s.getIsActive()))
                .toList();
        
        for (LiveSession activeSession : allActiveSessions) {
            // Check if this active session belongs to a course owned by this trainer
            try {
                CourseDTO course = courseClient.getCourseById(activeSession.getCourseId());
                if (course != null && trainerId.equals(course.getTrainerId())) {
                    return ResponseEntity.status(409).body(
                        "You already have an active live session running. Please end your current session before starting a new one."
                    );
                }
            } catch (Exception e) {
                // Continue checking other sessions if course fetch fails
            }
        }

        // Check if there's already an active session for this specific course
        List<LiveSession> existingSessions = liveSessionRepository.findByCourseId(session.getCourseId());
        boolean hasActiveSession = existingSessions.stream()
                .anyMatch(s -> Boolean.TRUE.equals(s.getIsActive()));
        
        if (hasActiveSession) {
            return ResponseEntity.status(409).body("There is already an active live session for this course. Please end the current session before starting a new one.");
        }

        session.setStartedAt(LocalDateTime.now());
        session.setIsActive(true);
        if (session.getChatEnabled() == null) {
            session.setChatEnabled(true);
        }

        if (session.getMeetingLink() == null || session.getMeetingLink().isBlank()) {
            String courseTitle;
            try {
                courseTitle = courseClient.getCourseTitle(session.getCourseId());
                courseTitle = courseTitle == null ? "LiveCourse" : courseTitle.replaceAll("[^a-zA-Z0-9]", "");
            } catch (Exception e) {
                courseTitle = "LiveCourse";
            }

            if (courseTitle.isBlank()) {
                courseTitle = "LiveCourse";
            }

            String roomName = courseTitle + "-" + UUID.randomUUID().toString().substring(0, 8);
            session.setMeetingLink("https://meet.jit.si/" + roomName);
        }

        LiveSession savedSession = liveSessionRepository.save(session);
        
        // Send session start notification to chat
        ChatMessage sessionStartMsg = new ChatMessage();
        sessionStartMsg.setSessionId(savedSession.getId());
        sessionStartMsg.setSenderId("SYSTEM");
        sessionStartMsg.setSenderName("System");
        sessionStartMsg.setContent("🎥 Live session has started! Welcome everyone!");
        sessionStartMsg.setTimestamp(LocalDateTime.now());
        chatMessageRepository.save(sessionStartMsg);

        return ResponseEntity.ok(savedSession);
    }

    @PostMapping("/create-simple")
    public ResponseEntity<?> createSessionSimple(@RequestBody LiveSession session, @RequestParam String trainerId) {
        log.info("========== CREATE SIMPLE SESSION REQUEST ==========");
        log.info("Received session: {}", session);
        log.info("Course ID: {}", session != null ? session.getCourseId() : "null");
        log.info("Trainer ID: {}", trainerId);
        
        try {
            if (session == null) {
                log.error("Session object is null");
                return ResponseEntity.badRequest().body(new ErrorResponse("Session data is required", 400));
            }
            
            if (session.getCourseId() == null) {
                log.error("Course ID is null");
                return ResponseEntity.badRequest().body(new ErrorResponse("Course ID is required", 400));
            }
            
            if (trainerId == null || trainerId.isBlank()) {
                log.error("Trainer ID is null or blank");
                return ResponseEntity.badRequest().body(new ErrorResponse("Trainer ID is required", 400));
            }

            log.info("Validating trainer access for course ID: {}", session.getCourseId());
            
            // Validate that the user is the trainer of this course
            if (!sessionAccessService.isCourseTrainer(session.getCourseId(), trainerId)) {
                log.error("User {} is not the trainer for course {}", trainerId, session.getCourseId());
                return ResponseEntity.status(403).body(new ErrorResponse("Only the trainer/creator of this course can create live sessions for it", 403));
            }

            log.info("Trainer validation passed. Checking for existing active sessions...");

            // Check if trainer has ANY active session across all courses
            List<LiveSession> allActiveSessions = liveSessionRepository.findAll().stream()
                    .filter(s -> Boolean.TRUE.equals(s.getIsActive()))
                    .toList();
            
            log.info("Found {} active sessions in total", allActiveSessions.size());
            
            for (LiveSession activeSession : allActiveSessions) {
                // Check if this active session belongs to a course owned by this trainer
                try {
                    CourseDTO course = courseClient.getCourseById(activeSession.getCourseId());
                    if (course != null && trainerId.equals(course.getTrainerId())) {
                        log.warn("Trainer {} already has an active session for course {}", trainerId, activeSession.getCourseId());
                        return ResponseEntity.status(409).body(new ErrorResponse(
                            "You already have an active live session running. Please end your current session before starting a new one.",
                            409
                        ));
                    }
                } catch (Exception e) {
                    log.warn("Could not fetch course {} for active session check: {}", activeSession.getCourseId(), e.getMessage());
                    // Continue checking other sessions if course fetch fails
                }
            }

            // Check if there's already an active session for this course
            List<LiveSession> existingSessions = liveSessionRepository.findByCourseId(session.getCourseId());
            boolean hasActiveSession = existingSessions.stream()
                    .anyMatch(s -> Boolean.TRUE.equals(s.getIsActive()));
            
            if (hasActiveSession) {
                log.warn("Course {} already has an active session", session.getCourseId());
                return ResponseEntity.status(409).body(new ErrorResponse(
                    "There is already an active live session for this course. Please end the current session before starting a new one.",
                    409
                ));
            }

            log.info("No conflicts found. Creating new session...");

            session.setStartedAt(LocalDateTime.now());
            session.setIsActive(true);
            if (session.getChatEnabled() == null) {
                session.setChatEnabled(true);
            }

            if (session.getMeetingLink() == null || session.getMeetingLink().isBlank()) {
                String courseTitle;
                try {
                    courseTitle = courseClient.getCourseTitle(session.getCourseId());
                    courseTitle = courseTitle == null ? "LiveCourse" : courseTitle.replaceAll("[^a-zA-Z0-9]", "");
                } catch (Exception e) {
                    log.warn("Could not fetch course title: {}", e.getMessage());
                    courseTitle = "LiveCourse";
                }

                if (courseTitle.isBlank()) {
                    courseTitle = "LiveCourse";
                }

                String roomName = courseTitle + "-" + UUID.randomUUID().toString().substring(0, 8);
                session.setMeetingLink("https://meet.jit.si/" + roomName);
                log.info("Generated meeting link: {}", session.getMeetingLink());
            }

            LiveSession savedSession = liveSessionRepository.save(session);
            log.info("Session created successfully with ID: {}", savedSession.getId());
            
            return ResponseEntity.ok(savedSession);
            
        } catch (Exception e) {
            log.error("Unexpected error creating session", e);
            return ResponseEntity.status(500).body(new ErrorResponse(
                "Internal server error: " + e.getMessage(),
                500
            ));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<LiveSessionResponse> getSession(@PathVariable Long id) {
        return liveSessionRepository.findById(id).map(session -> {
            String courseTitle;
            try {
                courseTitle = courseClient.getCourseTitle(session.getCourseId());
            } catch (Exception e) {
                courseTitle = "Course details unavailable";
            }
            return ResponseEntity.ok(new LiveSessionResponse(session, courseTitle));
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/join")
    public ResponseEntity<?> joinSession(@PathVariable Long id, @RequestParam String learnerId) {
        log.info("========== JOIN SESSION REQUEST ==========");
        log.info("Session ID: {}", id);
        log.info("Learner ID: {}", learnerId);
        
        try {
            if (learnerId == null || learnerId.isBlank()) {
                log.error("Learner ID is null or blank");
                return ResponseEntity.badRequest().body(new ErrorResponse("Learner ID is required", 400));
            }
            
            return liveSessionRepository.findById(id).map(session -> {
                if (!Boolean.TRUE.equals(session.getIsActive())) {
                    log.warn("Session {} is not active", id);
                    return ResponseEntity.status(403).body(new ErrorResponse("This live session is not active", 403));
                }

                // Check if user has paid enrollment for this course
                if (!sessionAccessService.hasPaidEnrollment(session.getCourseId(), learnerId)) {
                    log.warn("Learner {} does not have paid enrollment for course {}", learnerId, session.getCourseId());
                    return ResponseEntity.status(403).body(new ErrorResponse("Only learners who paid for this course can join this live session", 403));
                }

                String courseTitle;
                try {
                    courseTitle = courseClient.getCourseTitle(session.getCourseId());
                } catch (Exception e) {
                    log.warn("Could not fetch course title: {}", e.getMessage());
                    courseTitle = "Course details unavailable";
                }

                log.info("Learner {} successfully joined session {}", learnerId, id);
                return ResponseEntity.ok((Object) new LiveSessionResponse(session, courseTitle));
            }).orElse(ResponseEntity.notFound().build());
            
        } catch (Exception e) {
            log.error("Error joining session", e);
            return ResponseEntity.status(500).body(new ErrorResponse("Internal server error: " + e.getMessage(), 500));
        }
    }

    @GetMapping("/course/{courseId}")
    public List<LiveSessionResponse> getSessionsByCourse(@PathVariable Long courseId) {
        List<LiveSession> sessions = liveSessionRepository.findByCourseId(courseId);
        String courseTitle;
        try {
            courseTitle = courseClient.getCourseTitle(courseId);
        } catch (Exception e) {
            courseTitle = "Course details unavailable";
        }

        String finalCourseTitle = courseTitle;
        return sessions.stream()
                .map(session -> new LiveSessionResponse(session, finalCourseTitle))
                .toList();
    }

    @GetMapping("/course/{courseId}/total-time")
    public ResponseEntity<Map<String, Object>> getTotalLiveTime(@PathVariable Long courseId) {
        List<LiveSession> sessions = liveSessionRepository.findByCourseId(courseId);
        
        System.out.println("=== CALCULATING TOTAL LIVE TIME FOR COURSE " + courseId + " ===");
        System.out.println("Total sessions found: " + sessions.size());
        
        long totalSeconds = 0;
        int validSessionCount = 0; // Count sessions >= 30 minutes
        int totalSessionCount = 0; // Count all ended sessions
        
        for (LiveSession session : sessions) {
            System.out.println("\n--- Session ID: " + session.getId() + " ---");
            
            if (session.getStartedAt() == null) {
                System.out.println("❌ Skipped: No start time");
                continue;
            }
            
            // Only count ENDED sessions (sessions with endedAt timestamp)
            // Active sessions will be calculated in real-time by the frontend
            if (session.getEndedAt() == null) {
                System.out.println("❌ Skipped: Session still active (no end time)");
                continue; // Skip active or incomplete sessions
            }
            
            totalSessionCount++; // Count this ended session
            
            LocalDateTime startTime = session.getStartedAt();
            LocalDateTime endTime = session.getEndedAt();
            
            // Calculate duration in seconds
            long durationSeconds = java.time.Duration.between(startTime, endTime).getSeconds();
            long durationMinutes = durationSeconds / 60;
            
            System.out.println("Start: " + startTime);
            System.out.println("End: " + endTime);
            System.out.println("Duration: " + durationSeconds + " seconds (" + durationMinutes + " minutes)");
            
            // Only count sessions that are 30 minutes or longer (1800 seconds)
            if (durationSeconds >= 1800) {
                totalSeconds += durationSeconds;
                validSessionCount++;
                System.out.println("✅ COUNTED (>= 30 minutes)");
            } else {
                System.out.println("❌ SKIPPED (< 30 minutes)");
            }
        }
        
        System.out.println("\n=== SUMMARY ===");
        System.out.println("Total seconds: " + totalSeconds);
        System.out.println("Total minutes: " + (totalSeconds / 60));
        System.out.println("Valid sessions: " + validSessionCount);
        System.out.println("Formatted: " + formatDuration(totalSeconds));
        System.out.println("===============\n");
        
        Map<String, Object> response = new HashMap<>();
        response.put("courseId", courseId);
        response.put("totalSeconds", totalSeconds);
        response.put("totalMinutes", totalSeconds / 60);
        response.put("totalHours", totalSeconds / 3600);
        response.put("validSessionCount", validSessionCount); // Sessions >= 30 minutes
        response.put("totalSessionCount", totalSessionCount); // All ended sessions
        response.put("formattedTime", formatDuration(totalSeconds)); // Human-readable format
        
        return ResponseEntity.ok(response);
    }
    
    // Helper method to format duration in human-readable format
    private String formatDuration(long totalSeconds) {
        if (totalSeconds == 0) {
            return "0h 0m";
        }
        
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        
        if (hours > 0) {
            return String.format("%dh %dm", hours, minutes);
        } else {
            return String.format("%dm", minutes);
        }
    }

    @GetMapping("/course/user/{userId}")
    public List<LiveSessionResponse> getSessionsByUser(@PathVariable String userId) {
        return liveSessionRepository.findAll().stream()
                .filter(session -> {
                    try {
                        CourseDTO course = courseClient.getCourseById(session.getCourseId());
                        if (course != null && course.getTrainerId() != null) {
                            return userId.equals(course.getTrainerId());
                        }
                    } catch (Exception e) {
                        return false;
                    }
                    return false;
                })
                .map(session -> {
                    String courseTitle;
                    try {
                        courseTitle = courseClient.getCourseTitle(session.getCourseId());
                    } catch (Exception e) {
                        courseTitle = "Course details unavailable";
                    }
                    return new LiveSessionResponse(session, courseTitle);
                })
                .toList();
    }

    @GetMapping
    public List<LiveSessionResponse> getAllSessions() {
        return liveSessionRepository.findAll().stream().map(session -> {
            String courseTitle;
            try {
                courseTitle = courseClient.getCourseTitle(session.getCourseId());
            } catch (Exception e) {
                courseTitle = "Course details unavailable";
            }
            return new LiveSessionResponse(session, courseTitle);
        }).toList();
    }

    @PutMapping("/{id}/end")
    public ResponseEntity<?> endSession(@PathVariable Long id, @RequestParam String trainerId) {
        log.info("========== END SESSION REQUEST ==========");
        log.info("Session ID: {}", id);
        log.info("Trainer ID: {}", trainerId);
        
        try {
            if (trainerId == null || trainerId.isBlank()) {
                log.error("Trainer ID is null or blank");
                return ResponseEntity.badRequest().body(new ErrorResponse("Trainer ID is required", 400));
            }

            return liveSessionRepository.findById(id).map(session -> {
                if (!sessionAccessService.isCourseTrainer(session.getCourseId(), trainerId)) {
                    log.error("User {} is not the trainer for course {}", trainerId, session.getCourseId());
                    return ResponseEntity.status(403).body(new ErrorResponse("Only the course trainer can end this live session", 403));
                }

                session.setIsActive(false);
                session.setEndedAt(LocalDateTime.now());
                LiveSession savedSession = liveSessionRepository.save(session);
                log.info("Session {} ended successfully", id);
                
                // Auto-close all active polls for this session
                List<com.formini.msliveclass.entities.Poll> activePolls = pollRepository.findBySessionIdAndIsActive(id, true);
                for (com.formini.msliveclass.entities.Poll poll : activePolls) {
                    poll.setIsActive(false);
                    pollRepository.save(poll);
                    
                    // Send poll result announcement to chat
                    List<Map<String, Object>> optionResults = poll.getOptions().stream()
                            .map(option -> {
                                int index = poll.getOptions().indexOf(option);
                                long voteCount = pollVoteRepository.countByPollIdAndOptionIndex(poll.getId(), index);
                                Map<String, Object> optionResult = new HashMap<>();
                                optionResult.put("option", option);
                                optionResult.put("index", index);
                                optionResult.put("votes", voteCount);
                                return optionResult;
                            })
                            .collect(Collectors.toList());
                    
                    long totalVotes = optionResults.stream().mapToLong(r -> (Long)r.get("votes")).sum();
                    
                    if (!optionResults.isEmpty() && totalVotes > 0) {
                        long maxVotes = optionResults.stream()
                                .mapToLong(r -> (Long)r.get("votes"))
                                .max()
                                .orElse(0L);
                        
                        List<Map<String, Object>> winners = optionResults.stream()
                                .filter(r -> (Long)r.get("votes") == maxVotes)
                                .collect(Collectors.toList());
                        
                        com.formini.msliveclass.entities.ChatMessage announcement = new com.formini.msliveclass.entities.ChatMessage();
                        announcement.setSessionId(poll.getSessionId());
                        announcement.setSenderId("SYSTEM");
                        announcement.setSenderName("System");
                        
                        if (winners.size() > 1) {
                            String drawOptions = winners.stream()
                                    .map(w -> "\"" + w.get("option") + "\"")
                                    .collect(Collectors.joining(", "));
                            announcement.setContent("🤝 Poll ended! It's a draw between " + drawOptions + " with " + maxVotes + " vote" + (maxVotes != 1 ? "s" : "") + " each");
                        } else {
                            String winnerOption = (String) winners.get(0).get("option");
                            announcement.setContent("🏆 Poll ended! Winner: \"" + winnerOption + "\" with " + maxVotes + " vote" + (maxVotes != 1 ? "s" : ""));
                        }
                        
                        announcement.setTimestamp(LocalDateTime.now());
                        chatMessageRepository.save(announcement);
                    }
                }
                
                // Send session end notification to chat
                com.formini.msliveclass.entities.ChatMessage sessionEndMsg = new com.formini.msliveclass.entities.ChatMessage();
                sessionEndMsg.setSessionId(id);
                sessionEndMsg.setSenderId("SYSTEM");
                sessionEndMsg.setSenderName("System");
                sessionEndMsg.setContent("📺 Live session has ended. Thank you for participating!");
                sessionEndMsg.setTimestamp(LocalDateTime.now());
                chatMessageRepository.save(sessionEndMsg);
                
                return ResponseEntity.ok((Object) savedSession);
            }).orElse(ResponseEntity.notFound().build());
            
        } catch (Exception e) {
            log.error("Error ending session", e);
            return ResponseEntity.status(500).body(new ErrorResponse("Internal server error: " + e.getMessage(), 500));
        }
    }

    @PutMapping("/{id}/toggle-chat")
    public ResponseEntity<?> toggleChat(@PathVariable Long id, @RequestParam String trainerId) {
        log.info("========== TOGGLE CHAT REQUEST ==========");
        log.info("Session ID: {}", id);
        log.info("Trainer ID: {}", trainerId);
        
        try {
            if (trainerId == null || trainerId.isBlank()) {
                log.error("Trainer ID is null or blank");
                return ResponseEntity.badRequest().body(new ErrorResponse("Trainer ID is required", 400));
            }

            return liveSessionRepository.findById(id).map(session -> {
                if (!sessionAccessService.isCourseTrainer(session.getCourseId(), trainerId)) {
                    log.error("User {} is not the trainer for course {}", trainerId, session.getCourseId());
                    return ResponseEntity.status(403).body(new ErrorResponse("Only the course trainer can toggle chat for this session", 403));
                }

                Boolean isChatEnabled = session.getChatEnabled() == null ? true : session.getChatEnabled();
                session.setChatEnabled(!isChatEnabled);
                LiveSession updatedSession = liveSessionRepository.save(session);
                log.info("Chat toggled to {} for session {}", updatedSession.getChatEnabled(), id);
                return ResponseEntity.ok((Object) updatedSession);
            }).orElse(ResponseEntity.notFound().build());
            
        } catch (Exception e) {
            log.error("Error toggling chat", e);
            return ResponseEntity.status(500).body(new ErrorResponse("Internal server error: " + e.getMessage(), 500));
        }
    }
}
