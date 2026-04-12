package com.formini.msliveclass.controllers;

import com.formini.msliveclass.entities.Poll;
import com.formini.msliveclass.entities.PollVote;
import com.formini.msliveclass.entities.LiveSession;
import com.formini.msliveclass.entities.ChatMessage;
import com.formini.msliveclass.repositories.PollRepository;
import com.formini.msliveclass.repositories.PollVoteRepository;
import com.formini.msliveclass.repositories.LiveSessionRepository;
import com.formini.msliveclass.repositories.ChatMessageRepository;
import com.formini.msliveclass.services.SessionAccessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/poll")
public class PollController {

    private final PollRepository pollRepository;
    private final PollVoteRepository pollVoteRepository;
    private final LiveSessionRepository liveSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final SessionAccessService sessionAccessService;

    @Autowired
    public PollController(
            PollRepository pollRepository,
            PollVoteRepository pollVoteRepository,
            LiveSessionRepository liveSessionRepository,
            ChatMessageRepository chatMessageRepository,
            SessionAccessService sessionAccessService
    ) {
        this.pollRepository = pollRepository;
        this.pollVoteRepository = pollVoteRepository;
        this.liveSessionRepository = liveSessionRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.sessionAccessService = sessionAccessService;
    }

    @PostMapping("/create")
    public ResponseEntity<?> createPoll(@RequestBody Poll poll, @RequestParam String trainerId) {
        if (poll.getSessionId() == null) {
            return ResponseEntity.badRequest().body("Session ID is required.");
        }

        if (trainerId == null || trainerId.isBlank()) {
            return ResponseEntity.badRequest().body("Trainer ID is required.");
        }

        LiveSession session = liveSessionRepository.findById(poll.getSessionId()).orElse(null);
        if (session == null) {
            return ResponseEntity.badRequest().body("Session not found.");
        }

        if (!sessionAccessService.isCourseTrainer(session.getCourseId(), trainerId)) {
            return ResponseEntity.status(403).body("Only the trainer can create polls.");
        }

        if (poll.getQuestion() == null || poll.getQuestion().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Poll question is required.");
        }

        if (poll.getOptions() == null || poll.getOptions().size() < 2) {
            return ResponseEntity.badRequest().body("Poll must have at least 2 options.");
        }

        if (poll.getDurationSeconds() == null || poll.getDurationSeconds() < 10) {
            return ResponseEntity.badRequest().body("Poll duration must be at least 10 seconds.");
        }

        poll.setCreatorId(trainerId);
        poll.setCreatedAt(LocalDateTime.now());
        poll.setExpiresAt(LocalDateTime.now().plusSeconds(poll.getDurationSeconds()));
        poll.setIsActive(true);

        Poll savedPoll = pollRepository.save(poll);
        
        // Send automatic chat message announcing the poll
        ChatMessage announcement = new ChatMessage();
        announcement.setSessionId(poll.getSessionId());
        announcement.setSenderId("SYSTEM");
        announcement.setSenderName("System");
        announcement.setContent("📊 Poll started: " + poll.getQuestion());
        announcement.setTimestamp(LocalDateTime.now());
        chatMessageRepository.save(announcement);

        return ResponseEntity.ok(savedPoll);
    }

    @GetMapping("/session/{sessionId}")
    public ResponseEntity<?> getPollsBySession(@PathVariable Long sessionId, @RequestParam String userId) {
        LiveSession session = liveSessionRepository.findById(sessionId).orElse(null);
        if (session == null) {
            return ResponseEntity.badRequest().body("Session not found.");
        }

        boolean isTrainer = sessionAccessService.isCourseTrainer(session.getCourseId(), userId);
        boolean isPaidLearner = sessionAccessService.hasPaidEnrollment(session.getCourseId(), userId);

        if (!isTrainer && !isPaidLearner) {
            return ResponseEntity.status(403).body("Access denied.");
        }

        List<Poll> polls = pollRepository.findBySessionId(sessionId);
        
        // Auto-close expired polls
        LocalDateTime now = LocalDateTime.now();
        polls.forEach(poll -> {
            if (Boolean.TRUE.equals(poll.getIsActive()) && 
                poll.getExpiresAt() != null && 
                now.isAfter(poll.getExpiresAt())) {
                poll.setIsActive(false);
                pollRepository.save(poll);
            }
        });
        
        return ResponseEntity.ok(polls);
    }

    @GetMapping("/{pollId}/results")
    public ResponseEntity<?> getPollResults(@PathVariable Long pollId, @RequestParam String userId) {
        Poll poll = pollRepository.findById(pollId).orElse(null);
        if (poll == null) {
            return ResponseEntity.notFound().build();
        }

        // Auto-close if expired
        if (Boolean.TRUE.equals(poll.getIsActive()) && 
            poll.getExpiresAt() != null && 
            LocalDateTime.now().isAfter(poll.getExpiresAt())) {
            poll.setIsActive(false);
            pollRepository.save(poll);
        }

        LiveSession session = liveSessionRepository.findById(poll.getSessionId()).orElse(null);
        if (session == null) {
            return ResponseEntity.badRequest().body("Session not found.");
        }

        boolean isTrainer = sessionAccessService.isCourseTrainer(session.getCourseId(), userId);
        boolean isPaidLearner = sessionAccessService.hasPaidEnrollment(session.getCourseId(), userId);

        if (!isTrainer && !isPaidLearner) {
            return ResponseEntity.status(403).body("Access denied.");
        }

        Map<String, Object> results = new HashMap<>();
        results.put("poll", poll);
        
        List<Map<String, Object>> optionResults = poll.getOptions().stream()
                .map(option -> {
                    int index = poll.getOptions().indexOf(option);
                    long voteCount = pollVoteRepository.countByPollIdAndOptionIndex(pollId, index);
                    Map<String, Object> optionResult = new HashMap<>();
                    optionResult.put("option", option);
                    optionResult.put("index", index);
                    optionResult.put("votes", voteCount);
                    return optionResult;
                })
                .collect(Collectors.toList());
        
        results.put("results", optionResults);
        
        long totalVotes = pollVoteRepository.findByPollId(pollId).size();
        results.put("totalVotes", totalVotes);
        
        // Find winner (option with most votes) or detect draw
        if (!optionResults.isEmpty() && totalVotes > 0) {
            long maxVotes = optionResults.stream()
                    .mapToLong(r -> (Long)r.get("votes"))
                    .max()
                    .orElse(0L);
            
            // Check if there's a tie (multiple options with max votes)
            long winnersCount = optionResults.stream()
                    .filter(r -> (Long)r.get("votes") == maxVotes)
                    .count();
            
            if (winnersCount > 1) {
                // It's a draw
                results.put("isDraw", true);
                results.put("winner", null);
            } else {
                // Single winner
                Map<String, Object> winner = optionResults.stream()
                        .filter(r -> (Long)r.get("votes") == maxVotes)
                        .findFirst()
                        .orElse(null);
                results.put("isDraw", false);
                results.put("winner", winner);
            }
        } else {
            results.put("isDraw", false);
            results.put("winner", null);
        }
        
        // Check if user has voted
        boolean hasVoted = pollVoteRepository.findByPollIdAndVoterId(pollId, userId).isPresent();
        results.put("hasVoted", hasVoted);

        return ResponseEntity.ok(results);
    }

    @PostMapping("/{pollId}/vote")
    public ResponseEntity<?> vote(@PathVariable Long pollId, @RequestParam String userId, @RequestParam Integer optionIndex) {
        Poll poll = pollRepository.findById(pollId).orElse(null);
        if (poll == null) {
            return ResponseEntity.notFound().build();
        }

        if (!Boolean.TRUE.equals(poll.getIsActive())) {
            return ResponseEntity.status(403).body("This poll is closed.");
        }

        LiveSession session = liveSessionRepository.findById(poll.getSessionId()).orElse(null);
        if (session == null) {
            return ResponseEntity.badRequest().body("Session not found.");
        }

        boolean isPaidLearner = sessionAccessService.hasPaidEnrollment(session.getCourseId(), userId);
        if (!isPaidLearner) {
            return ResponseEntity.status(403).body("Only enrolled learners can vote.");
        }

        if (optionIndex < 0 || optionIndex >= poll.getOptions().size()) {
            return ResponseEntity.badRequest().body("Invalid option index.");
        }

        // Check if user already voted
        if (pollVoteRepository.findByPollIdAndVoterId(pollId, userId).isPresent()) {
            return ResponseEntity.status(409).body("You have already voted in this poll.");
        }

        PollVote vote = new PollVote();
        vote.setPollId(pollId);
        vote.setVoterId(userId);
        vote.setOptionIndex(optionIndex);
        vote.setVotedAt(LocalDateTime.now());

        return ResponseEntity.ok(pollVoteRepository.save(vote));
    }

    @PutMapping("/{pollId}/close")
    public ResponseEntity<?> closePoll(@PathVariable Long pollId, @RequestParam String trainerId) {
        Poll poll = pollRepository.findById(pollId).orElse(null);
        if (poll == null) {
            return ResponseEntity.notFound().build();
        }

        LiveSession session = liveSessionRepository.findById(poll.getSessionId()).orElse(null);
        if (session == null) {
            return ResponseEntity.badRequest().body("Session not found.");
        }

        if (!sessionAccessService.isCourseTrainer(session.getCourseId(), trainerId)) {
            return ResponseEntity.status(403).body("Only the trainer can close polls.");
        }

        poll.setIsActive(false);
        Poll closedPoll = pollRepository.save(poll);
        
        // Calculate winner and send announcement
        List<Map<String, Object>> optionResults = poll.getOptions().stream()
                .map(option -> {
                    int index = poll.getOptions().indexOf(option);
                    long voteCount = pollVoteRepository.countByPollIdAndOptionIndex(pollId, index);
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
            
            // Check if there's a tie
            List<Map<String, Object>> winners = optionResults.stream()
                    .filter(r -> (Long)r.get("votes") == maxVotes)
                    .collect(Collectors.toList());
            
            ChatMessage announcement = new ChatMessage();
            announcement.setSessionId(poll.getSessionId());
            announcement.setSenderId("SYSTEM");
            announcement.setSenderName("System");
            
            if (winners.size() > 1) {
                // It's a draw
                String drawOptions = winners.stream()
                        .map(w -> "\"" + w.get("option") + "\"")
                        .collect(Collectors.joining(", "));
                announcement.setContent("🤝 Poll ended! It's a draw between " + drawOptions + " with " + maxVotes + " vote" + (maxVotes != 1 ? "s" : "") + " each");
            } else {
                // Single winner
                String winnerOption = (String) winners.get(0).get("option");
                announcement.setContent("🏆 Poll ended! Winner: \"" + winnerOption + "\" with " + maxVotes + " vote" + (maxVotes != 1 ? "s" : ""));
            }
            
            announcement.setTimestamp(LocalDateTime.now());
            chatMessageRepository.save(announcement);
        }
        
        return ResponseEntity.ok(closedPoll);
    }
    
    @GetMapping("/session/{sessionId}/can-view")
    public ResponseEntity<?> canViewPolls(@PathVariable Long sessionId, @RequestParam String userId) {
        LiveSession session = liveSessionRepository.findById(sessionId).orElse(null);
        if (session == null) {
            return ResponseEntity.badRequest().body("Session not found.");
        }

        boolean isTrainer = sessionAccessService.isCourseTrainer(session.getCourseId(), userId);
        
        // Trainers can always view polls
        if (isTrainer) {
            return ResponseEntity.ok(Map.of("canView", true, "reason", "trainer"));
        }
        
        // Check if learner has paid enrollment
        boolean isPaidLearner = sessionAccessService.hasPaidEnrollment(session.getCourseId(), userId);
        if (!isPaidLearner) {
            return ResponseEntity.ok(Map.of("canView", false, "reason", "not_enrolled"));
        }
        
        // Check if learner has sent at least one message
        long messageCount = chatMessageRepository.countBySessionIdAndSenderId(sessionId, userId);
        boolean hasSentMessage = messageCount > 0;
        
        return ResponseEntity.ok(Map.of(
            "canView", hasSentMessage,
            "reason", hasSentMessage ? "has_chatted" : "no_messages",
            "messageCount", messageCount
        ));
    }
}
