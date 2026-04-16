package com.example.mstrainerhiring.utils;

import com.example.mstrainerhiring.enums.Technology;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

@Slf4j
public class ScoringEngine {

    private static final int TARGET_EXPERIENCE_YEARS = 5;
    private static final List<String> POSITIVE_KEYWORDS = Arrays.asList(
            "spring", "java", "api", "database", "sql", "git", "teamwork", "rest");
    private static final List<String> GREETINGS = Arrays.asList("hello", "dear", "hi", "greetings");
    private static final List<String> CLOSINGS = Arrays.asList("sincerely", "regards", "best", "thanks", "thank you");

    public static int calculateScore(Integer years, String motivation, Technology selectedTech) {
        if (motivation == null || motivation.trim().isEmpty()) {
            motivation = "";
        }

        double expScore = calculateExperienceScore(years);
        double letterScore = calculateLetterScore(motivation, selectedTech);

        // Weighted final score: 40% Experience, 60% Letter
        int finalScore = (int) Math.round((expScore * 0.4) + (letterScore * 0.6));

        return Math.min(100, Math.max(0, finalScore));
    }

    private static double calculateExperienceScore(Integer years) {
        if (years == null || years <= 0)
            return 0;
        if (years >= TARGET_EXPERIENCE_YEARS)
            return 100.0;
        return (years * 100.0) / TARGET_EXPERIENCE_YEARS;
    }

    private static double calculateLetterScore(String motivation, Technology selectedTech) {
        if (motivation.isEmpty())
            return 0;

        String normalized = motivation.toLowerCase();
        String[] words = normalized.split("\\s+");
        int wordCount = words.length;

        // 1. Effort Check (Max 20 pts)
        double effortPts = 0;
        if (wordCount > 100)
            effortPts = 20;
        else if (wordCount >= 50)
            effortPts = 10;

        // 2. Technical Skills (Max 60 pts)
        // Keywords (Max 40 pts)
        long distinctKeywordsFound = POSITIVE_KEYWORDS.stream()
                .filter(normalized::contains)
                .count();
        double keywordPts = Math.min(40, distinctKeywordsFound * 10);

        // Tech Match (Max 20 pts)
        double techMatchPts = 0;
        if (selectedTech != null && normalized.contains(selectedTech.name().toLowerCase())) {
            techMatchPts = 20;
        }

        // 3. Professionalism & Structure (Max 20 pts)
        // Etiquette (10 pts)
        boolean hasGreeting = GREETINGS.stream().anyMatch(normalized::contains);
        boolean hasClosing = CLOSINGS.stream().anyMatch(normalized::contains);
        double etiquettePts = (hasGreeting || hasClosing) ? 10 : 0;

        // Organization (10 pts)
        // Detection: presence of newlines or bullet points
        double organizationPts = (motivation.contains("\n\n") || motivation.contains("\r\n\r\n")
                || motivation.contains("- ") || motivation.contains("* ")) ? 10 : 0;

        return Math.min(100, effortPts + keywordPts + techMatchPts + etiquettePts + organizationPts);
    }
}
