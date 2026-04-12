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
 * AI Tutor Service - Personalized learning assistant
 * - Only helps with enrolled courses
 * - Provides detailed explanations
 * - Enforces profanity filter
 * - Friendly and engaging personality
 */
@Service
public class AITutorService {

    private static final Logger logger = LoggerFactory.getLogger(AITutorService.class);

    private final ChatClient chatClient;
    private final AIFunctionService aiFunctionService;
    private final ConversationHistoryService conversationHistoryService;
    private final ProfanityFilterService profanityFilterService;
    private final SessionAccessService sessionAccessService;

    @Autowired
    public AITutorService(ChatClient.Builder chatClientBuilder,
                         AIFunctionService aiFunctionService,
                         ConversationHistoryService conversationHistoryService,
                         ProfanityFilterService profanityFilterService,
                         SessionAccessService sessionAccessService) {
        this.chatClient = chatClientBuilder.build();
        this.aiFunctionService = aiFunctionService;
        this.conversationHistoryService = conversationHistoryService;
        this.profanityFilterService = profanityFilterService;
        this.sessionAccessService = sessionAccessService;
    }

    public String processMessage(String userMessage, String learnerId, String learnerName) {
        logger.info("📨 AI Tutor processing message from: {} ({})", learnerName, learnerId);
        
        try {
            // Check if user is banned
            if (profanityFilterService.isUserBanned(learnerId)) {
                return profanityFilterService.getBanMessage(learnerId);
            }
            
            // Check for profanity
            if (profanityFilterService.checkAndBanIfProfane(userMessage, learnerId)) {
                return profanityFilterService.getBanMessage(learnerId);
            }
            
            // Check if new conversation
            boolean isNewConversation = conversationHistoryService.isNewConversation(learnerId);
            
            // Add user message to history
            conversationHistoryService.addMessage(learnerId, "user", userMessage);
            
            // Get learner's PAID courses (from invoices/payments)
            String paidCoursesData = getPaidCourses(learnerId);
            
            // Detect intent and get relevant data
            String contextData = detectIntentAndGetData(userMessage, learnerId, paidCoursesData);
            
            // Build tutor prompt
            String systemPrompt = buildTutorPrompt(learnerId, learnerName, isNewConversation, 
                                                   paidCoursesData, contextData);
            
            // Get conversation context
            String conversationContext = buildConversationContext(learnerId);
            
            // Generate response
            String fullPrompt = systemPrompt + "\n\n" + conversationContext + 
                              "\n\nUser: " + userMessage + "\n\nAI Tutor:";
            
            String response = chatClient.prompt()
                .user(fullPrompt)
                .call()
                .content();
            
            if (response != null && !response.trim().isEmpty()) {
                response = response.trim();
                conversationHistoryService.addMessage(learnerId, "assistant", response);
                logger.info("✅ Tutor response generated");
                return response;
            } else {
                String fallback = "Hmm, I'm thinking... 🤔 Could you rephrase that? I want to make sure I give you the best answer!";
                conversationHistoryService.addMessage(learnerId, "assistant", fallback);
                return fallback;
            }
        } catch (Exception e) {
            logger.error("❌ Error: {}", e.getMessage(), e);
            String errorResponse = "Oops! My circuits got a bit tangled there. 😅 Let's try that again - what do you need help with?";
            conversationHistoryService.addMessage(learnerId, "assistant", errorResponse);
            return errorResponse;
        }
    }

    /**
     * Get paid courses for access control (checks actual payment status)
     */
    private String getPaidCourses(String learnerId) {
        try {
            logger.info("🔍 Fetching PAID courses for learner: {}", learnerId);
            
            // Get all enrollments
            var enrollmentsResponse = aiFunctionService.getLearnerEnrollments()
                .apply(createLearnerRequest(learnerId));
            
            if (enrollmentsResponse == null || enrollmentsResponse.result == null) {
                logger.warn("No enrollments found for learner: {}", learnerId);
                return "No paid courses found.";
            }
            
            // The enrollment response already contains only ACTIVE/COMPLETED enrollments
            // which means they are paid enrollments
            logger.info("✅ Found paid courses:\n{}", enrollmentsResponse.result);
            return enrollmentsResponse.result;
            
        } catch (Exception e) {
            logger.error("Error getting paid courses: {}", e.getMessage());
            return "No paid courses found.";
        }
    }

    /**
     * Detect intent and fetch relevant data with access control
     */
    private String detectIntentAndGetData(String userMessage, String learnerId, String paidCoursesData) {
        String message = userMessage.toLowerCase();
        StringBuilder data = new StringBuilder();
        
        try {
            // Always show PAID courses from database
            data.append("=== LEARNER'S PAID COURSES (from database) ===\n");
            data.append(paidCoursesData).append("\n");
            data.append("=== END PAID COURSES ===\n\n");
            
            // Check for specific course mentions
            String mentionedCourse = extractCourseMention(message);
            
            if (mentionedCourse != null) {
                logger.info("📚 Course topic mentioned: {}", mentionedCourse);
                
                // Check database if learner PAID for this course
                boolean hasPaid = isTopicInPaidCourses(mentionedCourse, paidCoursesData);
                
                if (hasPaid) {
                    logger.info("✅ Access granted - learner PAID for: {}", mentionedCourse);
                    
                    // Try to get course content
                    String courseContent = getCourseContentFromDatabase(learnerId, mentionedCourse, paidCoursesData);
                    
                    if (courseContent != null && !courseContent.isEmpty()) {
                        data.append("=== COURSE CONTENT (from database) ===\n");
                        data.append(courseContent).append("\n");
                        data.append("=== END COURSE CONTENT ===\n\n");
                    }
                    
                    // ALWAYS grant access if paid, even without detailed content
                    data.append("✅ ACCESS GRANTED: The learner has PAID for this course.\n");
                    data.append("You MUST explain and teach about ").append(mentionedCourse).append(" in detail.\n");
                    data.append("Use your knowledge to provide helpful explanations, examples, and guidance.\n");
                    data.append("If course chapters are shown above, reference them. Otherwise, use your general knowledge.\n\n");
                    
                } else {
                    logger.info("❌ Access denied - learner has NOT paid for: {}", mentionedCourse);
                    data.append("❌ ACCESS DENIED: Course '").append(mentionedCourse).append("' is NOT in the paid courses list above.\n");
                    data.append("You CANNOT explain this course. Politely tell the learner they need to enroll and pay for this course first.\n\n");
                }
            } else {
                // No specific course mentioned - general conversation
                data.append("💬 GENERAL CONVERSATION: No specific course topic detected.\n");
                data.append("Be friendly and helpful. Ask what they'd like to learn about from their paid courses.\n\n");
            }
            
        } catch (Exception e) {
            logger.error("❌ Error detecting intent: {}", e.getMessage(), e);
        }
        
        return data.toString();
    }

    /**
     * Get actual course content from database
     */
    private String getCourseContentFromDatabase(String learnerId, String topic, String paidCoursesData) {
        try {
            // Extract course ID from paid courses data
            Long courseId = extractCourseIdForTopic(topic, paidCoursesData);
            
            if (courseId == null) {
                logger.warn("Could not extract course ID for topic: {}", topic);
                return null;
            }
            
            logger.info("📖 Fetching course content for courseId: {} (topic: {})", courseId, topic);
            
            // Get chapters from database
            var courseRequest = new AIFunctionService.CourseRequest();
            courseRequest.learnerId = learnerId;
            courseRequest.courseId = courseId;
            
            var chaptersResponse = aiFunctionService.getCourseChapters().apply(courseRequest);
            
            if (chaptersResponse != null && chaptersResponse.result != null) {
                StringBuilder content = new StringBuilder();
                content.append("Course Topic: ").append(topic).append("\n");
                content.append("Course ID: ").append(courseId).append("\n\n");
                content.append(chaptersResponse.result);
                
                logger.info("✅ Successfully fetched course content");
                return content.toString();
            } else {
                logger.warn("No chapters found for course ID: {}", courseId);
            }
            
        } catch (Exception e) {
            logger.error("Error fetching course content: {}", e.getMessage());
        }
        
        return null;
    }

    /**
     * Extract course ID from paid courses data
     */
    private Long extractCourseIdForTopic(String topic, String paidCoursesData) {
        // This is a simple extraction - you may need to enhance based on your data format
        // Looking for patterns like "courseId: 123" or similar
        try {
            String[] lines = paidCoursesData.split("\n");
            boolean foundTopic = false;
            
            for (String line : lines) {
                if (line.toLowerCase().contains(topic.toLowerCase())) {
                    foundTopic = true;
                }
                
                // If we found the topic, look for course ID in nearby lines
                if (foundTopic && line.contains("courseId")) {
                    String[] parts = line.split(":");
                    if (parts.length > 1) {
                        String idStr = parts[1].trim().replaceAll("[^0-9]", "");
                        if (!idStr.isEmpty()) {
                            return Long.parseLong(idStr);
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error extracting course ID: {}", e.getMessage());
        }
        
        return null;
    }

    /**
     * Extract course mention from message
     */
    private String extractCourseMention(String message) {
        // Common course topics and technologies
        Pattern coursePattern = Pattern.compile(
            "\\b(docker|kubernetes|k8s|angular|react|vue|java|python|javascript|typescript|" +
            "c\\+\\+|cpp|c#|csharp|ruby|php|go|golang|rust|swift|kotlin|" +
            "spring|node|express|django|flask|laravel|rails|" +
            "mongodb|sql|mysql|postgresql|redis|elasticsearch|" +
            "css|html|sass|less|bootstrap|tailwind|" +
            "git|linux|aws|azure|gcp|devops|ci/cd|jenkins|" +
            "microservices|api|rest|graphql|grpc|" +
            "testing|security|authentication|oauth|jwt|" +
            "machine learning|ml|ai|data science|analytics)\\b",
            Pattern.CASE_INSENSITIVE
        );
        
        Matcher matcher = coursePattern.matcher(message);
        if (matcher.find()) {
            String match = matcher.group(1).toLowerCase();
            // Normalize variations
            if (match.equals("k8s")) return "kubernetes";
            if (match.equals("cpp")) return "c++";
            if (match.equals("csharp")) return "c#";
            if (match.equals("golang")) return "go";
            
            logger.info("🎯 Extracted course topic: {}", match);
            return match;
        }
        
        logger.info("ℹ️ No specific course topic detected in message");
        return null;
    }

    /**
     * Check if topic is in paid courses
     */
    private boolean isTopicInPaidCourses(String topic, String paidCoursesData) {
        if (topic == null || paidCoursesData == null) {
            return false;
        }
        
        String lowerData = paidCoursesData.toLowerCase();
        String lowerTopic = topic.toLowerCase();
        
        // Check if the topic appears in the paid courses data
        boolean found = lowerData.contains(lowerTopic);
        
        if (found) {
            logger.info("✅ Topic '{}' found in paid courses", topic);
        } else {
            logger.info("❌ Topic '{}' NOT found in paid courses", topic);
        }
        
        return found;
    }

    /**
     * Build conversation context
     */
    private String buildConversationContext(String learnerId) {
        List<ConversationMessage> history = conversationHistoryService.getHistory(learnerId);
        
        if (history.size() <= 1) {
            return "";
        }
        
        StringBuilder context = new StringBuilder("RECENT CONVERSATION:\n");
        int start = Math.max(0, history.size() - 6);
        for (int i = start; i < history.size() - 1; i++) {
            ConversationMessage msg = history.get(i);
            String role = "user".equals(msg.getRole()) ? "User" : "AI Tutor";
            context.append(role).append(": ").append(msg.getContent()).append("\n");
        }
        
        return context.toString();
    }

    /**
     * Build AI tutor system prompt - let AI make decisions based on real data
     */
    private String buildTutorPrompt(String learnerId, String learnerName, boolean isNewConversation,
                                   String paidCoursesData, String contextData) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("You are a friendly, knowledgeable AI tutor for ").append(learnerName != null ? learnerName : "the learner").append(".\n\n");
        
        prompt.append("YOUR MISSION:\n");
        prompt.append("- Teach and explain topics from the learner's PAID courses\n");
        prompt.append("- Provide detailed explanations, examples, and step-by-step guidance\n");
        prompt.append("- Be conversational, friendly, and encouraging\n");
        prompt.append("- Use emojis sparingly to keep it fun\n");
        prompt.append("- Break down complex concepts into simple terms\n\n");
        
        prompt.append("CRITICAL ACCESS RULES:\n");
        prompt.append("1. Check the database data below for '✅ ACCESS GRANTED' or '❌ ACCESS DENIED'\n");
        prompt.append("2. If ACCESS GRANTED: Teach the topic thoroughly using your knowledge\n");
        prompt.append("3. If ACCESS DENIED: Politely say they need to enroll in that course first\n");
        prompt.append("4. If no specific topic mentioned: Be friendly and ask what they want to learn\n\n");
        
        prompt.append("HOW TO TEACH:\n");
        prompt.append("- Start with a brief overview of the concept\n");
        prompt.append("- Provide practical examples and use cases\n");
        prompt.append("- Explain step-by-step when appropriate\n");
        prompt.append("- Ask if they want more details or have questions\n");
        prompt.append("- Reference course chapters if provided in the data\n\n");
        
        // Add real database data
        if (contextData != null && !contextData.trim().isEmpty()) {
            prompt.append("=== DATABASE DATA ===\n");
            prompt.append(contextData);
            prompt.append("=== END DATABASE DATA ===\n\n");
        }
        
        prompt.append("Now respond to the learner's message. If you see ACCESS GRANTED, teach them about the topic!\n\n");
        
        return prompt.toString();
    }

    /**
     * Clear conversation
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
