package com.example.mstrainerhiring.services.impl;

import com.example.mstrainerhiring.entities.Job;
import com.example.mstrainerhiring.entities.TrainerHiring;
import com.example.mstrainerhiring.enums.TrainerStatus;
import com.example.mstrainerhiring.repositories.TrainerHiringRepository;
import com.example.mstrainerhiring.services.IntelligenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class IntelligenceServiceImpl implements IntelligenceService {

    private final TrainerHiringRepository trainerRepository;

    @Override
    public String extractTextFromPdf(String filePath) {
        try (PDDocument document = Loader.loadPDF(new File(filePath))) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        } catch (IOException e) {
            log.error("Failed to extract text from PDF: {}", filePath, e);
            return "";
        }
    }

    @Override
    public TrainerHiring analyzeApplication(TrainerHiring application, String cvText) {
        log.info("Starting intelligence analysis for application: {}", application.getId());

        // 0. Blank CV check
        boolean isBlank = cvText == null || cvText.trim().isEmpty();
        application.setIsBlankCv(isBlank);

        // 1. Skill Sync Score
        double skillSync = isBlank ? 0.0 : calculateSkillSync(application, cvText);
        application.setSkillSyncScore(skillSync);

        // 2. Red-Flag Plagiarism detection
        boolean isPlagiarized = isBlank ? false : detectPlagiarism(application);
        application.setPlagiarismFlag(isPlagiarized);

        // 3. Tone & Clarity Analysis
        double toneScore = isBlank ? 0.0 : analyzeToneAndClarity(application.getMotivationLetter());
        application.setToneClarityScore(toneScore);

        // 4. Hiring Trend Prediction
        double probability = isBlank ? 0.0 : predictAcceptanceProbability(application);
        application.setAcceptanceProbability(probability);

        // 5. Synthesis context
        String context = generateIntelligentRecommendation(skillSync, toneScore, isPlagiarized, probability, isBlank);
        application.setIntelligentAnalysisContext(context);

        return application;
    }

    private String generateIntelligentRecommendation(double skillSync, double tone, boolean plagiarized,
            double probability, boolean isBlank) {
        StringBuilder sb = new StringBuilder();
        sb.append("Intelligent verification completed. ");

        if (isBlank) {
            sb.append(
                    "CRITICAL: The provided CV document is blank or unreadable. Application cannot be processed further. ");
            return sb.toString();
        }

        if (plagiarized) {
            sb.append("CAUTION: Significant linguistic overlap detected with other records. ");
        }

        if (skillSync > 80) {
            sb.append("Exceptional technical alignment identified. ");
        } else if (skillSync > 50) {
            sb.append("Adequate technical alignment for the role. ");
        }

        if (tone > 80) {
            sb.append("Highly professional communication style. ");
        } else if (tone < 40) {
            sb.append("Communication style may lack formal structure. ");
        }

        sb.append(String.format("Calculated success probability index: %.1f%%.", probability));

        return sb.toString();
    }

    private double calculateSkillSync(TrainerHiring application, String cvText) {
        Job job = application.getJob();
        if (job == null)
            return 0.0;

        String content = (cvText + " " + application.getMotivationLetter()).toLowerCase();
        double score = 0;

        // Technology match (40%)
        if (content.contains(job.getTechnology().name().toLowerCase())) {
            score += 40;
        }

        // Experience match (60%)
        if (application.getYearsOfExperience() != null) {
            int required = job.getMinExperience() != null ? job.getMinExperience() : 2;
            if (application.getYearsOfExperience() >= required) {
                score += 60;
            } else {
                score += (application.getYearsOfExperience() * 60.0) / required;
            }
        }

        return Math.min(100, score);
    }

    private boolean detectPlagiarism(TrainerHiring current) {
        List<TrainerHiring> others = trainerRepository.findAll();
        for (TrainerHiring other : others) {
            if (other.getId().equals(current.getId()))
                continue;

            double similarity = calculateSimilarity(current.getMotivationLetter(), other.getMotivationLetter());
            if (similarity > 0.50) { // 50% similarity threshold (User Request)
                log.warn("Plagiarism detected between {} and {}", current.getId(), other.getId());
                return true;
            }
        }
        return false;
    }

    private double analyzeToneAndClarity(String text) {
        if (text == null || text.trim().isEmpty())
            return 0.0;

        // Clean text for counting
        String cleanText = text.trim();

        // Ensure at least one sentence if text exists but lacks punctuation
        String[] sentenceParts = cleanText.split("[.!?]+");
        int sentences = Math.max(1, sentenceParts.length);

        // Word counting
        String[] wordParts = cleanText.split("\\s+");
        int words = Math.max(1, wordParts.length);

        int syllables = countSyllables(cleanText);

        // Flesch Reading Ease Formula
        double score = 206.835 - 1.015 * ((double) words / sentences) - 84.6 * ((double) syllables / words);

        log.info("Tone Analysis - Words: {}, Sentences: {}, Syllables: {}, Score: {}", words, sentences, syllables,
                score);

        // Mapping to Professionalism Percentage (0-100)
        // High scores (90+) are very easy/simple.
        // Professional content usually falls in 30-60 range.
        if (score >= 30 && score <= 70)
            return 100.0; // Perfect professional balance
        if (score < 30)
            return 85.0; // Complex/Academic (Professional enough)
        if (score > 70 && score <= 90)
            return 60.0; // Fairly simple
        return 40.0; // Very basic/Incomplete
    }

    private double predictAcceptanceProbability(TrainerHiring application) {
        // Statistical Trend logic: Compare with avg experience of ACCEPTED candidates
        List<TrainerHiring> accepted = trainerRepository.findByStatus(TrainerStatus.ACCEPTED);
        if (accepted.isEmpty())
            return 50.0; // Baseline

        double avgExp = accepted.stream()
                .mapToInt(a -> a.getYearsOfExperience() != null ? a.getYearsOfExperience() : 0)
                .average().orElse(0.0);

        double prob = 50.0;
        if (application.getYearsOfExperience() != null) {
            if (application.getYearsOfExperience() >= avgExp)
                prob += 30;
            else
                prob += (application.getYearsOfExperience() * 30.0) / avgExp;
        }

        // Adjust by Skill Sync
        if (application.getSkillSyncScore() != null) {
            prob = (prob + application.getSkillSyncScore()) / 2;
        }

        return Math.min(95, prob);
    }

    @Override
    public double calculateSimilarity(String s1, String s2) {
        if (s1 == null || s2 == null)
            return 0.0;
        int distance = levenshteinDistance(s1.toLowerCase(), s2.toLowerCase());
        return 1.0 - ((double) distance / Math.max(s1.length(), s2.length()));
    }

    private int levenshteinDistance(String x, String y) {
        int[][] dp = new int[x.length() + 1][y.length() + 1];
        for (int i = 0; i <= x.length(); i++)
            dp[i][0] = i;
        for (int j = 0; j <= y.length(); j++)
            dp[0][j] = j;

        for (int i = 1; i <= x.length(); i++) {
            for (int j = 1; j <= y.length(); j++) {
                if (x.charAt(i - 1) == y.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    dp[i][j] = 1 + Math.min(dp[i - 1][j - 1], Math.min(dp[i - 1][j], dp[i][j - 1]));
                }
            }
        }
        return dp[x.length()][y.length()];
    }

    private int countSyllables(String text) {
        String work = text.toLowerCase().replaceAll("[^a-z ]", "");
        int count = 0;
        String[] words = work.split("\\s+");
        for (String word : words) {
            if (word.isEmpty())
                continue;
            count += countWordSyllables(word);
        }
        return count;
    }

    private int countWordSyllables(String word) {
        Pattern p = Pattern.compile("[aeiouy]+");
        Matcher m = p.matcher(word);
        int count = 0;
        while (m.find())
            count++;
        if (word.endsWith("e"))
            count--;
        return Math.max(1, count);
    }
}
