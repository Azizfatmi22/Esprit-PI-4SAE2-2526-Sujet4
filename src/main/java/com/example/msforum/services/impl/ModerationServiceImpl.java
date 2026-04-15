package com.example.msforum.services.impl;

import com.example.msforum.entities.ContentStatus;
import com.example.msforum.services.ModerationService;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class ModerationServiceImpl implements ModerationService {

    // A sample list of bad words. In a real app, this should probably be in a database or external file.
    private static final Set<String> BAD_WORDS = new HashSet<>(Arrays.asList(
        // English
        "fuck", "shit", "asshole", "bitch", "crap", "bastard",
        // French
        "merde", "connard", "salope", "putain", "cul", "batard",
        // Tunisian (Transliterated & Arabic)
        "zibi", "masta", "khra", "asba", "nayek", "farkh",
        "زبّي", "منيك", "خرا", "عصبة"
    ));

    private static final int MAX_LINKS = 2;
    private static final int REPEATED_WORD_THRESHOLD = 3;
    private static final Pattern URL_PATTERN = Pattern.compile("https?://\\S+|www\\.\\S+");

    @Override
    public ContentStatus moderateContent(String text) {
        if (text == null || text.isBlank()) {
            return ContentStatus.APPROVED;
        }

        String normalizedText = text.toLowerCase();

        // 1. Check for offensive content (REJECTED)
        if (containsBadWords(normalizedText)) {
            return ContentStatus.REJECTED;
        }

        // 2. Detect Spam (PENDING)
        if (isSpam(normalizedText)) {
            return ContentStatus.PENDING;
        }

        return ContentStatus.APPROVED;
    }

    private boolean containsBadWords(String text) {
        // Simple word-based check. For more robustness, use regex or a more advanced library.
        String[] words = text.split("\\s+");
        for (String word : words) {
            // Remove punctuation for check
            String cleanWord = word.replaceAll("[^a-zA-Z0-9\u0600-\u06FF]", "");
            if (BAD_WORDS.contains(cleanWord)) {
                return true;
            }
        }
        return false;
    }

    private boolean isSpam(String text) {
        // A. Too many links
        Matcher matcher = URL_PATTERN.matcher(text);
        int linksCount = 0;
        while (matcher.find()) {
            linksCount++;
        }
        if (linksCount > MAX_LINKS) {
            return true;
        }

        // B. Repeated words (e.g., "buy buy buy buy")
        String[] words = text.split("\\s+");
        if (words.length > 5) {
            for (int i = 0; i < words.length - REPEATED_WORD_THRESHOLD; i++) {
                boolean allSame = true;
                for (int j = 1; j <= REPEATED_WORD_THRESHOLD; j++) {
                    if (!words[i].equals(words[i + j])) {
                        allSame = false;
                        break;
                    }
                }
                if (allSame && words[i].length() > 2) { // Only care if word length > 2
                    return true;
                }
            }
        }

        return false;
    }
}
