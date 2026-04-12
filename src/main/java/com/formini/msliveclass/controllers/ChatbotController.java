package com.formini.msliveclass.controllers;

import com.formini.msliveclass.dto.ChatbotRequest;
import com.formini.msliveclass.dto.ChatbotResponse;
import com.formini.msliveclass.services.AITutorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/chatbot")
public class ChatbotController {

    private final AITutorService tutorService;

    @Autowired
    public ChatbotController(AITutorService tutorService) {
        this.tutorService = tutorService;
    }

    @PostMapping("/chat")
    public ResponseEntity<ChatbotResponse> chat(@RequestBody ChatbotRequest request) {
        if (request.getMessage() == null || request.getMessage().trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(new ChatbotResponse("Please provide a message.", getCurrentTimestamp()));
        }

        String learnerId = request.getLearnerId();
        String learnerName = request.getLearnerName();
        
        System.out.println("🎓 AI Tutor request:");
        System.out.println("   Message: " + request.getMessage());
        System.out.println("   Learner: " + learnerName + " (" + learnerId + ")");
        
        String response = tutorService.processMessage(request.getMessage(), learnerId, learnerName);
        return ResponseEntity.ok(new ChatbotResponse(response, getCurrentTimestamp()));
    }

    @PostMapping("/clear")
    public ResponseEntity<String> clearConversation(@RequestParam String learnerId) {
        if (learnerId == null || learnerId.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Learner ID is required");
        }
        
        tutorService.clearConversation(learnerId);
        return ResponseEntity.ok("Conversation cleared successfully");
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Chatbot service is running!");
    }

    private String getCurrentTimestamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
}
