package com.formini.msliveclass.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.formini.msliveclass.dto.ConversationMessage;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Simplified Chatbot Service without complex function calling
 * Uses direct function invocation based on user intent
 */
@Service
public class SimpleChatbotService {

    private static final Logger logger = LoggerFactory.getLogger(SimpleChatbotService.class);

    private final ChatClient chatClient;
    private final AIFunctionService aiFunctionService;
    private final ConversationHistoryService conversationHistoryService;

    @Autowired
    public SimpleChatbotService(ChatClient.Builder chatClientBuilder,
                                AIFunctionService aiFunctionService,
                                ConversationHistoryService conversationHistoryService) {
        this.chatClient = chatClientBuilder.build();
        this.aiFunctionService = aiFunctionService;
        this.conversationHistoryService = conversationHistoryService;
    }

    public String processMessage(String userMessage, String learnerId, String learnerName) {
        logger.info("📨 Processing message from: {} ({})", learnerName, learnerId);
        
        try {
            // Check if new conversation
            boolean isNewConversation = conversationHistoryService.isNewConversation(learnerId);
            
            // Add user message to history
            conversationHistoryService.addMessage(learnerId, "user", userMessage);
            
            // Detect intent and get data if needed
            String contextData = detectIntentAndGetData(userMessage, learnerId);
            
            // Build conversational prompt
            String systemPrompt = buildSimplePrompt(learnerId, learnerName, isNewConversation, contextData);
            
            // Get conversation history
            String conversationContext = buildConversationContext(learnerId);
            
            // Generate response
            String fullPrompt = systemPrompt + "\n\n" + conversationContext + "\n\nUser: " + userMessage + "\n\nAssistant:";
            
            String response = chatClient.prompt()
                .user(fullPrompt)
                .call()
                .content();
            
            if (response != null && !response.trim().isEmpty()) {
                // Clean up response
                response = response.trim();
                
                // Add to history
                conversationHistoryService.addMessage(learnerId, "assistant", response);
                
                logger.info("✅ Response generated successfully");
                return response;
            } else {
                String fallback = "I'm thinking... 🤔 Could you rephrase that?";
                conversationHistoryService.addMessage(learnerId, "assistant", fallback);
                return fallback;
            }
        } catch (Exception e) {
            logger.error("❌ Error: {}", e.getMessage(), e);
            String errorResponse = "Oops! I encountered a hiccup. 😅 Let's try that again - what would you like to know?";
            conversationHistoryService.addMessage(learnerId, "assistant", errorResponse);
            return errorResponse;
        }
    }

    /**
     * Detect user intent and fetch relevant data
     */
    private String detectIntentAndGetData(String userMessage, String learnerId) {
        String message = userMessage.toLowerCase();
        StringBuilder data = new StringBuilder();
        
        try {
            // Check for enrollment/progress queries (expanded keywords)
            if (message.contains("enrolled") || message.contains("my courses") || 
                message.contains("my progress") || message.contains("what am i taking") ||
                message.contains("courses i") || message.contains("courses im") ||
                message.contains("my enrollment") || message.contains("taking") ||
                message.contains("studying") || message.contains("learning")) {
                
                logger.info("🎯 Intent: Get enrollments");
                var enrollments = aiFunctionService.getLearnerEnrollments()
                    .apply(createLearnerRequest(learnerId));
                data.append("\n\nLEARNER'S ACTUAL ENROLLMENTS FROM DATABASE:\n").append(enrollments.result);
                logger.info("📊 Retrieved enrollment data: {}", enrollments.result);
            }
            
            // Check for course list queries (expanded keywords)
            if (message.contains("available courses") || message.contains("all courses") ||
                message.contains("what courses") || message.contains("course list") ||
                message.contains("show courses") || message.contains("see courses") ||
                message.contains("browse courses") || message.contains("course catalog")) {
                
                logger.info("🎯 Intent: Get all courses");
                var courses = aiFunctionService.getAllCourses()
                    .apply(new AIFunctionService.Request());
                data.append("\n\nALL AVAILABLE COURSES FROM DATABASE:\n").append(courses.result);
                logger.info("📊 Retrieved courses data");
            }
            
            // Check for platform stats
            if (message.contains("how many") || message.contains("statistics") ||
                message.contains("platform") || message.contains("learners") ||
                message.contains("stats") || message.contains("total")) {
                
                logger.info("🎯 Intent: Get platform stats");
                var stats = aiFunctionService.getPlatformStatistics()
                    .apply(new AIFunctionService.Request());
                data.append("\n\nPLATFORM STATISTICS FROM DATABASE:\n").append(stats.result);
                logger.info("📊 Retrieved platform stats");
            }
            
            // Check for course search
            Pattern searchPattern = Pattern.compile("(find|search|show|get|about).*?(docker|angular|java|python|spring|react|node|javascript|typescript|css|html)", Pattern.CASE_INSENSITIVE);
            Matcher matcher = searchPattern.matcher(userMessage);
            if (matcher.find()) {
                String keyword = matcher.group(2);
                logger.info("🎯 Intent: Search courses for: {}", keyword);
                
                var searchRequest = new AIFunctionService.SearchRequest();
                searchRequest.keyword = keyword;
                var results = aiFunctionService.searchCourses().apply(searchRequest);
                data.append("\n\nSEARCH RESULTS FROM DATABASE FOR '").append(keyword).append("':\n").append(results.result);
                logger.info("📊 Retrieved search results for: {}", keyword);
            }
            
        } catch (Exception e) {
            logger.error("❌ Error fetching data: {}", e.getMessage(), e);
        }
        
        if (data.length() == 0) {
            logger.warn("⚠️ No intent detected for message: {}", userMessage);
        }
        
        return data.toString();
    }

    /**
     * Build conversation context from history
     */
    private String buildConversationContext(String learnerId) {
        List<ConversationMessage> history = conversationHistoryService.getHistory(learnerId);
        
        if (history.size() <= 1) {
            return "";
        }
        
        StringBuilder context = new StringBuilder("RECENT CONVERSATION:\n");
        
        // Get last few messages (excluding current)
        int start = Math.max(0, history.size() - 6);
        for (int i = start; i < history.size() - 1; i++) {
            ConversationMessage msg = history.get(i);
            String role = "user".equals(msg.getRole()) ? "User" : "Assistant";
            context.append(role).append(": ").append(msg.getContent()).append("\n");
        }
        
        return context.toString();
    }

    /**
     * Build simple conversational prompt
     */
    private String buildSimplePrompt(String learnerId, String learnerName, boolean isNewConversation, String contextData) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("You are Formini AI, a friendly learning assistant! 🎓\n\n");
        
        prompt.append("CRITICAL RULES - READ CAREFULLY:\n");
        prompt.append("1. ONLY use data provided in the 'RELEVANT DATA' section below\n");
        prompt.append("2. NEVER make up or invent course names, enrollments, or statistics\n");
        prompt.append("3. If no data is provided, say you don't have that information and offer to help with something else\n");
        prompt.append("4. NEVER mention courses like 'Introduction to Programming', 'Data Structures', 'Web Development' unless they appear in the provided data\n");
        prompt.append("5. Be honest if you don't have the requested information\n\n");
        
        prompt.append("PERSONALITY:\n");
        prompt.append("- Warm, supportive, and encouraging\n");
        prompt.append("- Use casual, friendly language\n");
        prompt.append("- Show genuine interest in learning progress\n");
        prompt.append("- Use emojis naturally\n");
        prompt.append("- Be conversational, not robotic\n\n");
        
        if (learnerName != null && !learnerName.trim().isEmpty()) {
            prompt.append("CURRENT LEARNER: ").append(learnerName).append("\n");
            if (isNewConversation) {
                prompt.append("Status: New conversation - greet warmly!\n\n");
            } else {
                prompt.append("Status: Continuing conversation - reference previous context\n\n");
            }
        }
        
        prompt.append("RESPONSE GUIDELINES:\n");
        prompt.append("- Keep responses concise (2-4 sentences)\n");
        prompt.append("- Use bullet points for lists\n");
        prompt.append("- End with engaging questions or suggestions\n");
        prompt.append("- Match user's energy level\n");
        prompt.append("- For casual greetings like 'yo' or 'hey', respond warmly and ask how you can help\n");
        prompt.append("- Reference conversation history naturally\n");
        prompt.append("- Celebrate progress and encourage learning\n\n");
        
        if (contextData != null && !contextData.trim().isEmpty()) {
            prompt.append("=== RELEVANT DATA FROM DATABASE ===\n");
            prompt.append(contextData);
            prompt.append("\n=== END OF DATA ===\n\n");
            prompt.append("IMPORTANT: Use ONLY the data above to answer. Do not add or invent any information.\n");
            prompt.append("If the data shows 'No enrollments found' or is empty, tell the user you don't see any enrollments yet.\n\n");
        } else {
            prompt.append("=== NO SPECIFIC DATA PROVIDED ===\n");
            prompt.append("The user's question doesn't require database information, OR no relevant data was found.\n");
            prompt.append("- For greetings: Respond warmly and offer to help\n");
            prompt.append("- For general questions: Provide helpful explanations using your knowledge\n");
            prompt.append("- For course-specific questions: Explain you don't have that information and suggest they check their dashboard\n\n");
        }
        
        prompt.append("Remember: Be natural, friendly, and ONLY use the data provided above. Never invent information!\n\n");
        
        return prompt.toString();
    }

    /**
     * Clear conversation history
     */
    public void clearConversation(String learnerId) {
        conversationHistoryService.clearHistory(learnerId);
        logger.info("🗑️ Cleared conversation for: {}", learnerId);
    }

    /**
     * Helper to create learner request
     */
    private AIFunctionService.LearnerRequest createLearnerRequest(String learnerId) {
        AIFunctionService.LearnerRequest request = new AIFunctionService.LearnerRequest();
        request.learnerId = learnerId;
        return request;
    }
}
