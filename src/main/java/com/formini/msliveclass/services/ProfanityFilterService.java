package com.formini.msliveclass.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Profanity Filter and Ban Management Service
 * Detects inappropriate language and temporarily bans users
 */
@Service
public class ProfanityFilterService {

    private static final Logger logger = LoggerFactory.getLogger(ProfanityFilterService.class);
    
    // Store banned users with expiry time
    private final Map<String, LocalDateTime> bannedUsers = new ConcurrentHashMap<>();
    
    // Ban duration in minutes
    private static final int BAN_DURATION_MINUTES = 30;
    
    // Profanity patterns (basic list - expand as needed)
    private static final Set<String> PROFANITY_WORDS = new HashSet<>(Arrays.asList(
        "fuck", "shit", "bitch", "ass", "damn", "hell", "crap", "piss",
        "bastard", "dick", "cock", "pussy", "whore", "slut", "fag",
        "nigger", "nigga", "retard", "idiot", "stupid", "dumb"
    ));
    
    // Compile patterns for efficient matching
    private final List<Pattern> profanityPatterns;
    
    public ProfanityFilterService() {
        profanityPatterns = new ArrayList<>();
        for (String word : PROFANITY_WORDS) {
            // Match word with optional special characters and variations
            String pattern = "\\b" + word.replace("", "[*@#$%]*") + "\\b";
            profanityPatterns.add(Pattern.compile(pattern, Pattern.CASE_INSENSITIVE));
        }
    }

    /**
     * Check if user is currently banned
     */
    public boolean isUserBanned(String learnerId) {
        if (learnerId == null || learnerId.trim().isEmpty()) {
            return false;
        }
        
        LocalDateTime banExpiry = bannedUsers.get(learnerId);
        if (banExpiry == null) {
            return false;
        }
        
        // Check if ban has expired
        if (LocalDateTime.now().isAfter(banExpiry)) {
            bannedUsers.remove(learnerId);
            logger.info("✅ Ban expired for learner: {}", learnerId);
            return false;
        }
        
        return true;
    }

    /**
     * Get remaining ban time in minutes
     */
    public long getRemainingBanMinutes(String learnerId) {
        LocalDateTime banExpiry = bannedUsers.get(learnerId);
        if (banExpiry == null) {
            return 0;
        }
        
        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(banExpiry)) {
            return 0;
        }
        
        return java.time.Duration.between(now, banExpiry).toMinutes();
    }

    /**
     * Check message for profanity and ban user if found
     * Returns true if profanity detected
     */
    public boolean checkAndBanIfProfane(String message, String learnerId) {
        if (message == null || message.trim().isEmpty()) {
            return false;
        }
        
        // Check for profanity
        String lowerMessage = message.toLowerCase();
        for (Pattern pattern : profanityPatterns) {
            if (pattern.matcher(lowerMessage).find()) {
                // Profanity detected - ban user
                banUser(learnerId);
                logger.warn("🚫 Profanity detected from learner: {}. Message: {}", learnerId, message);
                return true;
            }
        }
        
        return false;
    }

    /**
     * Ban a user for the specified duration
     */
    private void banUser(String learnerId) {
        LocalDateTime banExpiry = LocalDateTime.now().plusMinutes(BAN_DURATION_MINUTES);
        bannedUsers.put(learnerId, banExpiry);
        logger.info("⛔ Banned learner {} until {}", learnerId, banExpiry);
    }

    /**
     * Manually unban a user (admin function)
     */
    public void unbanUser(String learnerId) {
        bannedUsers.remove(learnerId);
        logger.info("✅ Manually unbanned learner: {}", learnerId);
    }

    /**
     * Get ban expiry message
     */
    public String getBanMessage(String learnerId) {
        long remainingMinutes = getRemainingBanMinutes(learnerId);
        if (remainingMinutes <= 0) {
            return "";
        }
        
        return String.format(
            "🚫 You've been temporarily restricted from using the AI assistant due to inappropriate language.\n\n" +
            "⏰ Time remaining: %d minutes\n\n" +
            "Please use respectful language when the restriction is lifted. " +
            "Our AI is here to help you learn! 📚",
            remainingMinutes
        );
    }
}
