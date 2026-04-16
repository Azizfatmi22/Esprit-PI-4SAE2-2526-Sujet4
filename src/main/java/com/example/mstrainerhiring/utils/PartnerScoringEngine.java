package com.example.mstrainerhiring.utils;

import com.example.mstrainerhiring.entities.PartnerHiring;
import com.example.mstrainerhiring.enums.PartnerTier;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class PartnerScoringEngine {

    private static final List<String> BLACKLISTED_DOMAINS = Arrays.asList(
            "tempmail", "yopmail", "mailinator", "10minutemail", "guerrillamail",
            "throwaway", "fakeinbox", "sharklasers", "dispostable", "trashmail");

    private static final List<String> TRUSTED_TLDS = Arrays.asList(
            ".com", ".org", ".net", ".edu", ".gov", ".tn", ".fr", ".de", ".uk", ".io");

    private static final List<String> SUSPICIOUS_WORDS = Arrays.asList(
            "test", "fake", "demo", "sample", "asdf", "qwerty", "xxx", "temp", "null", "undefined");

    private static final Pattern VALID_PHONE_PATTERN = Pattern.compile("^[2579][0-9]{7}$");
    private static final Pattern HTTPS_PATTERN = Pattern.compile("^https://.*");
    private static final Pattern HTTP_PATTERN = Pattern.compile("^https?://.*");

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class ScoringResult {
        private int score;
        private String analysis;
    }

    public static ScoringResult calculateTrustScore(PartnerHiring partner, boolean hasLogo) {
        int score = 0;
        StringBuilder analysis = new StringBuilder();
        analysis.append("═══════════════════════════════════════\n");
        analysis.append("       PARTNER TRUST ENGINE v2.0       \n");
        analysis.append("═══════════════════════════════════════\n\n");

        // ─────────────────────────────────────
        // PILLAR 1: IDENTITY VERIFICATION (max 25 pts)
        // ─────────────────────────────────────
        analysis.append("▸ PILLAR 1 — Identity Verification\n");

        if (hasLogo) {
            score += 15;
            analysis.append("  ✅ [+15] Official logo uploaded — visual identity confirmed\n");
        } else {
            analysis.append("  ⚠️ [+0]  No logo — unverifiable visual identity\n");
        }

        if (partner.getOrganizationName() != null) {
            String name = partner.getOrganizationName().trim();
            if (name.length() >= 5 && !containsSuspiciousWords(name)) {
                score += 10;
                analysis.append("  ✅ [+10] Organization name \"" + name + "\" passes integrity check\n");
            } else if (name.length() >= 3) {
                score += 3;
                analysis.append("  ⚠️ [+3]  Organization name is short or borderline\n");
            } else {
                analysis.append("  ❌ [+0]  Organization name too short or suspicious\n");
            }
        }

        // ─────────────────────────────────────
        // PILLAR 2: DIGITAL PRESENCE (max 25 pts)
        // ─────────────────────────────────────
        analysis.append("\n▸ PILLAR 2 — Digital Presence\n");

        if (partner.getWebsite() != null && !partner.getWebsite().trim().isEmpty()) {
            String website = partner.getWebsite().trim().toLowerCase();

            if (HTTPS_PATTERN.matcher(website).matches()) {
                score += 15;
                analysis.append("  ✅ [+15] HTTPS-secured website detected\n");
            } else if (HTTP_PATTERN.matcher(website).matches()) {
                score += 5;
                analysis.append("  ⚠️ [+5]  Website present but lacks HTTPS encryption\n");
            } else {
                score += 2;
                analysis.append("  ⚠️ [+2]  Website value provided but not a valid URL format\n");
            }

            // TLD quality check
            boolean hasTrustedTld = TRUSTED_TLDS.stream().anyMatch(website::endsWith);
            if (hasTrustedTld) {
                score += 5;
                analysis.append("  ✅ [+5]  Recognized top-level domain (TLD)\n");
            } else {
                analysis.append("  ⚠️ [+0]  Uncommon or unrecognized TLD\n");
            }

            // Domain-email cross-reference
            if (partner.getEmail() != null) {
                String emailDomain = partner.getEmail().split("@").length > 1
                        ? partner.getEmail().split("@")[1].toLowerCase() : "";
                if (!emailDomain.isEmpty() && website.contains(emailDomain)) {
                    score += 5;
                    analysis.append("  ✅ [+5]  Email domain matches website — strong authenticity signal\n");
                } else {
                    analysis.append("  ⚠️ [+0]  Email domain does not match website\n");
                }
            }
        } else {
            analysis.append("  ❌ [+0]  No website provided — zero digital footprint\n");
        }

        // ─────────────────────────────────────
        // PILLAR 3: CONTACT RELIABILITY (max 20 pts)
        // ─────────────────────────────────────
        analysis.append("\n▸ PILLAR 3 — Contact Reliability\n");

        if (partner.getEmail() != null) {
            String email = partner.getEmail().toLowerCase();
            boolean isBlacklisted = BLACKLISTED_DOMAINS.stream().anyMatch(email::contains);
            if (isBlacklisted) {
                score -= 10;
                analysis.append("  🚨 [-10] DISPOSABLE EMAIL DETECTED — high fraud risk\n");
            } else if (email.endsWith(".edu") || email.endsWith(".gov") || email.endsWith(".tn")) {
                score += 10;
                analysis.append("  ✅ [+10] Institutional/government email — high trust\n");
            } else {
                score += 5;
                analysis.append("  ✅ [+5]  Standard email provider\n");
            }
        }

        if (partner.getPhone() != null && VALID_PHONE_PATTERN.matcher(partner.getPhone()).matches()) {
            score += 10;
            analysis.append("  ✅ [+10] Valid Tunisian phone number format\n");
        } else {
            analysis.append("  ⚠️ [+0]  Phone number missing or invalid format\n");
        }

        // ─────────────────────────────────────
        // PILLAR 4: CORPORATE TRANSPARENCY (max 20 pts)
        // ─────────────────────────────────────
        analysis.append("\n▸ PILLAR 4 — Corporate Transparency\n");

        if (partner.getAddress() != null) {
            int addrLength = partner.getAddress().trim().length();
            if (addrLength >= 25) {
                score += 10;
                analysis.append("  ✅ [+10] Detailed corporate address provided (" + addrLength + " chars)\n");
            } else if (addrLength >= 10) {
                score += 5;
                analysis.append("  ⚠️ [+5]  Address provided but lacks detail\n");
            } else {
                analysis.append("  ⚠️ [+0]  Address too vague to verify\n");
            }
        }

        if (partner.getLegalForm() != null) {
            score += 5;
            analysis.append("  ✅ [+5]  Legal form declared: " + partner.getLegalForm() + "\n");
        }

        if (partner.getPartnershipType() != null) {
            score += 5;
            analysis.append("  ✅ [+5]  Partnership type specified: " + partner.getPartnershipType() + "\n");
        }

        // ─────────────────────────────────────
        // PILLAR 5: DOCUMENT COMPLETENESS (max 10 pts)
        // ─────────────────────────────────────
        analysis.append("\n▸ PILLAR 5 — Document Completeness\n");

        int docCount = partner.getDocuments() != null ? partner.getDocuments().size() : 0;
        if (docCount >= 2) {
            score += 10;
            analysis.append("  ✅ [+10] Full document set uploaded (" + docCount + " documents)\n");
        } else if (docCount == 1) {
            score += 5;
            analysis.append("  ⚠️ [+5]  Partial documentation (" + docCount + "/2 required)\n");
        } else {
            analysis.append("  ❌ [+0]  No supporting documents uploaded\n");
        }

        // ─────────────────────────────────────
        // FRAUD SIGNALS (penalties)
        // ─────────────────────────────────────
        analysis.append("\n▸ FRAUD SIGNAL ANALYSIS\n");

        boolean suspicious = isSuspicious(partner);
        if (suspicious) {
            score -= 15;
            analysis.append("  🚨 [-15] ANOMALY DETECTED — pattern analysis flagged suspicious input\n");
        } else {
            analysis.append("  ✅ [+0]  No anomalies detected in input patterns\n");
        }

        // Name/email coherence: simple heuristic
        if (partner.getOrganizationName() != null && partner.getEmail() != null) {
            String nameNorm = partner.getOrganizationName().toLowerCase().replaceAll("[^a-z]", "");
            String emailPrefix = partner.getEmail().split("@")[0].toLowerCase().replaceAll("[^a-z]", "");
            // Check if any 4+ char substring of org name appears in email
            boolean hasOverlap = false;
            if (nameNorm.length() >= 4) {
                for (int i = 0; i <= nameNorm.length() - 4; i++) {
                    if (emailPrefix.contains(nameNorm.substring(i, i + 4))) {
                        hasOverlap = true;
                        break;
                    }
                }
            }
            if (hasOverlap) {
                score += 5;
                analysis.append("  ✅ [+5]  Organization name correlates with email — authenticity signal\n");
            }
        }

        // ─────────────────────────────────────
        // FINAL VERDICT
        // ─────────────────────────────────────
        int finalScore = Math.min(100, Math.max(0, score));
        PartnerTier determinedTier = determineTier(finalScore);

        analysis.append("\n═══════════════════════════════════════\n");
        analysis.append("  FINAL SCORE: ").append(finalScore).append("/100\n");
        analysis.append("  ASSIGNED TIER: ").append(determinedTier).append("\n");
        analysis.append("  RISK LEVEL: ").append(getRiskLevel(finalScore)).append("\n");
        analysis.append("═══════════════════════════════════════");

        return new ScoringResult(finalScore, analysis.toString());
    }

    public static PartnerTier determineTier(int trustScore) {
        if (trustScore >= 80) return PartnerTier.PLATINUM;
        if (trustScore >= 55) return PartnerTier.GOLD;
        if (trustScore >= 30) return PartnerTier.SILVER;
        return PartnerTier.BRONZE;
    }

    private static String getRiskLevel(int score) {
        if (score >= 75) return "LOW ✅";
        if (score >= 50) return "MODERATE ⚠️";
        if (score >= 25) return "HIGH 🔶";
        return "CRITICAL 🚨";
    }

    public static boolean isSuspicious(PartnerHiring partner) {
        // 1. Disposable email check
        if (partner.getEmail() != null) {
            String email = partner.getEmail().toLowerCase();
            for (String domain : BLACKLISTED_DOMAINS) {
                if (email.contains(domain)) {
                    return true;
                }
            }
        }

        // 2. Keyboard smash detection (6+ consecutive consonants)
        if (partner.getOrganizationName() != null) {
            String name = partner.getOrganizationName().toLowerCase();
            int consonantCount = 0;
            for (char ch : name.toCharArray()) {
                if (Character.isLetter(ch) && "aeiou".indexOf(ch) == -1) {
                    consonantCount++;
                } else {
                    consonantCount = 0;
                }
                if (consonantCount >= 5) {
                    return true;
                }
            }
        }

        // 3. Suspicious keyword detection
        if (partner.getOrganizationName() != null) {
            if (containsSuspiciousWords(partner.getOrganizationName())) {
                return true;
            }
        }

        // 4. Repeated character detection (e.g. "aaaaaaa")
        if (partner.getOrganizationName() != null) {
            String name = partner.getOrganizationName().toLowerCase();
            int repeatCount = 1;
            for (int i = 1; i < name.length(); i++) {
                if (name.charAt(i) == name.charAt(i - 1)) {
                    repeatCount++;
                    if (repeatCount >= 4) return true;
                } else {
                    repeatCount = 1;
                }
            }
        }

        return false;
    }

    private static boolean containsSuspiciousWords(String text) {
        String lower = text.toLowerCase();
        return SUSPICIOUS_WORDS.stream().anyMatch(lower::contains);
    }
}
