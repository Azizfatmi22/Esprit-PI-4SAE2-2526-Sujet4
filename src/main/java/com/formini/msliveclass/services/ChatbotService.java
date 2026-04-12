package com.formini.msliveclass.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.ai.model.function.FunctionCallbackWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.formini.msliveclass.dto.ConversationMessage;

import java.util.ArrayList;
import java.util.List;

/**
 * Chatbot Service with Conversation Memory and Function Calling
 * Provides an engaging, context-aware learning assistant
 */
@Service
public class ChatbotService {

    private static final Logger logger = LoggerFactory.getLogger(ChatbotService.class);

    private final ChatClient chatClient;
    private final AIFunctionService aiFunctionService;
    private final ConversationHistoryService conversationHistoryService;

    @Autowired
    public ChatbotService(ChatClient.Builder chatClientBuilder,
                         AIFunctionService aiFunctionService,
                         ConversationHistoryService conversationHistoryService) {
        this.chatClient = chatClientBuilder.build();
        this.aiFunctionService = aiFunctionService;
        this.conversationHistoryService = conversationHistoryService;
    }

    public String processMessage(String userMessage, String learnerId, String learnerName) {
        logger.info("📨 Processing message from learner: {} ({})", learnerName, learnerId);
        
        try {
            // Check if this is a new conversation
            boolean isNewConversation = conversationHistoryService.isNewConversation(learnerId);
            
            // Add user message to history
            conversationHistoryService.addMessage(learnerId, "user", userMessage);
            
            // Build system prompt with conversation context
            String systemPrompt = buildConversationalSystemPrompt(learnerId, learnerName, isNewConversation);
            
            // Get conversation history for context
            List<ConversationMessage> history = conversationHistoryService.getHistory(learnerId);
            
            // Create messages list with history
            List<Message> messages = new ArrayList<>();
            
            // Add conversation history (excluding the current message)
            for (int i = 0; i < history.size() - 1; i++) {
                ConversationMessage msg = history.get(i);
                if ("user".equals(msg.getRole())) {
                    messages.add(new UserMessage(msg.getContent()));
                } else {
                    messages.add(new AssistantMessage(msg.getContent()));
                }
            }
            
            // Add current user message
            messages.add(new UserMessage(userMessage));
            
            // Create function callbacks
            List<FunctionCallback> functionCallbacks = createFunctionCallbacks(learnerId);
            
            logger.info("🔧 Registered {} functions, {} history messages", 
                functionCallbacks.size(), history.size() - 1);
            
            // Build prompt with system message and conversation history
            Prompt prompt = new Prompt(messages);
            
            // Call AI with functions
            String response = chatClient.prompt()
                .system(systemPrompt)
                .user(userMessage)
                .functions(functionCallbacks.toArray(new FunctionCallback[0]))
                .call()
                .content();
            
            if (response != null && !response.trim().isEmpty()) {
                // Add assistant response to history
                conversationHistoryService.addMessage(learnerId, "assistant", response);
                
                logger.info("✅ AI response generated successfully");
                return response;
            } else {
                logger.warn("⚠️ AI returned empty response");
                String fallback = "I'm thinking... 🤔 Could you rephrase that or ask me something else?";
                conversationHistoryService.addMessage(learnerId, "assistant", fallback);
                return fallback;
            }
        } catch (Exception e) {
            logger.error("❌ AI Error: {}", e.getMessage(), e);
            String errorResponse = "Oops! I encountered a technical hiccup. 😅\n\n" +
                   "Let's try that again - what would you like to know?";
            conversationHistoryService.addMessage(learnerId, "assistant", errorResponse);
            return errorResponse;
        }
    }

    /**
     * Clear conversation history for a learner
     */
    public void clearConversation(String learnerId) {
        conversationHistoryService.clearHistory(learnerId);
        logger.info("🗑️ Cleared conversation for learner: {}", learnerId);
    }

    /**
     * Create function callbacks that the AI can call to query the database
     */
    private List<FunctionCallback> createFunctionCallbacks(String learnerId) {
        List<FunctionCallback> callbacks = new ArrayList<>();
        
        // Function 1: Get all courses (PUBLIC)
        callbacks.add(FunctionCallbackWrapper.builder(aiFunctionService.getAllCourses())
            .withName("getAllCourses")
            .withDescription("Returns list of all available courses. No parameters needed.")
            .build());
        
        // Function 2: Get learner's enrollments (PRIVATE)
        String enrollmentDesc = "Returns courses the learner is enrolled in. ";
        if (learnerId != null && !learnerId.trim().isEmpty()) {
            enrollmentDesc += "Parameter: learnerId='" + learnerId + "' (use this exact value)";
        } else {
            enrollmentDesc += "Requires learnerId parameter.";
        }
        callbacks.add(FunctionCallbackWrapper.builder(aiFunctionService.getLearnerEnrollments())
            .withName("getLearnerEnrollments")
            .withDescription(enrollmentDesc)
            .withInputType(AIFunctionService.LearnerRequest.class)
            .build());
        
        // Function 3: Get course chapters (PRIVATE - requires enrollment)
        String chaptersDesc = "Returns chapters for a course. ";
        if (learnerId != null && !learnerId.trim().isEmpty()) {
            chaptersDesc += "Parameters: learnerId='" + learnerId + "', courseId=<number>";
        } else {
            chaptersDesc += "Requires learnerId and courseId parameters.";
        }
        callbacks.add(FunctionCallbackWrapper.builder(aiFunctionService.getCourseChapters())
            .withName("getCourseChapters")
            .withDescription(chaptersDesc)
            .withInputType(AIFunctionService.CourseRequest.class)
            .build());
        
        // Function 4: Get chapter content (PRIVATE - requires enrollment)
        String contentDesc = "Returns detailed chapter content. ";
        if (learnerId != null && !learnerId.trim().isEmpty()) {
            contentDesc += "Parameters: learnerId='" + learnerId + "', courseId=<number>, chapterId=<number>";
        } else {
            contentDesc += "Requires learnerId, courseId, and chapterId parameters.";
        }
        callbacks.add(FunctionCallbackWrapper.builder(aiFunctionService.getChapterContent())
            .withName("getChapterContent")
            .withDescription(contentDesc)
            .withInputType(AIFunctionService.ChapterRequest.class)
            .build());
        
        // Function 5: Get platform statistics (PUBLIC)
        callbacks.add(FunctionCallbackWrapper.builder(aiFunctionService.getPlatformStatistics())
            .withName("getPlatformStatistics")
            .withDescription("Returns platform statistics (total courses, learners, enrollments). No parameters needed.")
            .build());
        
        // Function 6: Search courses (PUBLIC)
        callbacks.add(FunctionCallbackWrapper.builder(aiFunctionService.searchCourses())
            .withName("searchCourses")
            .withDescription("Search courses by keyword. Parameter: keyword=<search_term>")
            .withInputType(AIFunctionService.SearchRequest.class)
            .build());
        
        return callbacks;
    }

    /**
     * Build conversational system prompt with personality and engagement
     */
    private String buildConversationalSystemPrompt(String learnerId, String learnerName, boolean isNewConversation) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("You are Formini AI, a friendly and enthusiastic learning assistant! 🎓\n\n");
        
        prompt.append("YOUR PERSONALITY:\n");
        prompt.append("- Warm, supportive, and encouraging\n");
        prompt.append("- Use casual, friendly language (like talking to a friend)\n");
        prompt.append("- Show genuine interest in the learner's progress\n");
        prompt.append("- Celebrate achievements and milestones\n");
        prompt.append("- Use emojis naturally to add warmth\n");
        prompt.append("- Be patient and never condescending\n");
        prompt.append("- Show enthusiasm for learning and growth\n\n");
        
        // User context
        if (learnerId != null && !learnerId.trim().isEmpty()) {
            prompt.append("CURRENT LEARNER:\n");
            if (learnerName != null && !learnerName.trim().isEmpty()) {
                prompt.append("Name: ").append(learnerName).append("\n");
                if (isNewConversation) {
                    prompt.append("Status: Starting a new conversation - greet them warmly!\n");
                } else {
                    prompt.append("Status: Continuing our conversation - reference previous context naturally\n");
                }
            }
            prompt.append("ID: ").append(learnerId).append(" (use for function calls, never mention to user)\n\n");
        }
        
        prompt.append("YOUR CAPABILITIES:\n\n");
        
        prompt.append("1. 📚 Course Information (getAllCourses)\n");
        prompt.append("   - Show all available courses\n");
        prompt.append("   - Recommend courses based on interests\n");
        prompt.append("   - Explain course benefits and content\n\n");
        
        prompt.append("2. 🎯 Personal Progress (getLearnerEnrollments)\n");
        prompt.append("   - Show enrolled courses and progress\n");
        prompt.append("   - Celebrate milestones and achievements\n");
        prompt.append("   - Provide encouragement and motivation\n");
        if (learnerId != null && !learnerId.trim().isEmpty()) {
            prompt.append("   - Parameter: learnerId=\"").append(learnerId).append("\"\n");
        }
        prompt.append("\n");
        
        prompt.append("3. 📖 Course Structure (getCourseChapters)\n");
        prompt.append("   - Show chapters and lessons\n");
        prompt.append("   - Explain learning path\n");
        prompt.append("   - Suggest next steps\n");
        if (learnerId != null && !learnerId.trim().isEmpty()) {
            prompt.append("   - Parameters: learnerId=\"").append(learnerId).append("\", courseId=<number>\n");
        }
        prompt.append("\n");
        
        prompt.append("4. 💡 Lesson Content (getChapterContent)\n");
        prompt.append("   - Explain lesson details\n");
        prompt.append("   - Break down complex topics\n");
        prompt.append("   - Provide learning tips\n");
        if (learnerId != null && !learnerId.trim().isEmpty()) {
            prompt.append("   - Parameters: learnerId=\"").append(learnerId).append("\", courseId=<number>, chapterId=<number>\n");
        }
        prompt.append("\n");
        
        prompt.append("5. 📊 Platform Stats (getPlatformStatistics)\n");
        prompt.append("   - Share community size and activity\n");
        prompt.append("   - Show learning trends\n");
        prompt.append("   - Build sense of community\n\n");
        
        prompt.append("6. 🔍 Course Search (searchCourses)\n");
        prompt.append("   - Find courses by topic\n");
        prompt.append("   - Recommend relevant courses\n");
        prompt.append("   - Parameter: keyword=<search_term>\n\n");
        
        prompt.append("CONVERSATION GUIDELINES:\n\n");
        
        prompt.append("Opening Conversations:\n");
        prompt.append("- Greet warmly and ask how you can help\n");
        prompt.append("- Show enthusiasm about their learning journey\n");
        prompt.append("- Offer specific suggestions based on context\n\n");
        
        prompt.append("During Conversations:\n");
        prompt.append("- Remember and reference previous messages naturally\n");
        prompt.append("- Ask follow-up questions to understand better\n");
        prompt.append("- Provide context-aware suggestions\n");
        prompt.append("- Show you're listening and engaged\n\n");
        
        prompt.append("Handling Questions:\n");
        prompt.append("- ALWAYS call functions to get real data - never make up information\n");
        prompt.append("- If you need more info, ask clarifying questions\n");
        prompt.append("- For general learning questions, use your knowledge to help\n");
        prompt.append("- Explain concepts in simple, relatable terms\n\n");
        
        prompt.append("Engagement Strategies:\n");
        prompt.append("- Celebrate progress: 'Awesome! You're 75% through that course! 🎉'\n");
        prompt.append("- Encourage next steps: 'Ready to tackle the next chapter?'\n");
        prompt.append("- Show empathy: 'I know that topic can be tricky, let me help!'\n");
        prompt.append("- Be conversational: 'Yo! What's up?' → 'Hey! How can I help you today?'\n");
        prompt.append("- Offer help proactively: 'Want me to explain that concept?'\n\n");
        
        prompt.append("Response Style:\n");
        prompt.append("- Keep responses concise but complete (2-4 sentences usually)\n");
        prompt.append("- Use bullet points for lists\n");
        prompt.append("- Add relevant emojis naturally (not every sentence)\n");
        prompt.append("- End with an engaging question or suggestion\n");
        if (learnerName != null && !learnerName.trim().isEmpty()) {
            prompt.append("- Address them as \"").append(learnerName).append("\" occasionally\n");
        }
        prompt.append("- Match their energy level (casual → casual, formal → friendly-professional)\n\n");
        
        prompt.append("IMPORTANT RULES:\n");
        prompt.append("1. NEVER mention user IDs or technical details\n");
        prompt.append("2. ALWAYS use functions to get real data about courses/progress\n");
        prompt.append("3. Be helpful even with casual messages like 'yo' or 'help'\n");
        prompt.append("4. If stuck, ask clarifying questions rather than giving up\n");
        prompt.append("5. Remember conversation context and reference it naturally\n");
        prompt.append("6. Show personality - be warm, not robotic\n");
        prompt.append("7. Encourage learning and celebrate every step forward\n\n");
        
        prompt.append("EXAMPLE INTERACTIONS:\n\n");
        
        prompt.append("User: 'yo bro can u help me'\n");
        prompt.append("You: 'Hey! Of course I can help! 😊 What do you need? Want to check your courses, learn something new, or just chat about your learning goals?'\n\n");
        
        prompt.append("User: 'what am i enrolled in'\n");
        prompt.append("You: [Call getLearnerEnrollments] 'You're enrolled in 3 awesome courses! 🎓 [list them with progress]. Which one would you like to focus on today?'\n\n");
        
        prompt.append("User: 'explain docker to me'\n");
        prompt.append("You: 'Great question! Docker is like a shipping container for your code... [explain clearly]. Want to see our Docker course? It covers all this and more!'\n\n");
        
        prompt.append("Now, engage with the learner naturally and helpfully!\n");
        
        return prompt.toString();
    }
}
