package tn.esprit.microservice.reclamation.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/msreclamation/translation")
public class TranslationController {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${anthropic.api.key}")
    private String geminiApiKey;

    @PostMapping("/detect")
    public ResponseEntity<?> detectLanguage(@RequestBody Map<String, String> payload) {
        String text = payload.getOrDefault("text", "").trim();
        String lang = detectLang(text);
        String[] info = getLangInfo(lang);
        return ResponseEntity.ok(Map.of(
                "detectedLanguage", lang,
                "languageName",     info[0],
                "languageFlag",     info[1],
                "isFrench",         "FR".equals(lang)
        ));
    }

    @PostMapping("/translate")
    public ResponseEntity<?> translate(@RequestBody Map<String, String> payload) {
        String text       = payload.getOrDefault("text", "").trim();
        String targetLang = payload.getOrDefault("targetLang", "français");

        if (text.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Texte vide"));
        }

        String detectedLang = detectLang(text);
        String[] langInfo   = getLangInfo(detectedLang);
        String sourceCode   = getLangIsoCode(detectedLang);
        String targetCode   = getTargetIsoCode(targetLang);

        // ── 1. Essayer Gemini 2.0 Flash ──────────────────────────
        try {
            String prompt = "Translate the following text to " + targetLang +
                    ". Return ONLY the translation, nothing else:\n\n" + text;

            String geminiUrl = "https://generativelanguage.googleapis.com/v1beta/models/"
                    + "gemini-2.0-flash:generateContent?key=" + geminiApiKey;

            Map<String, Object> body = Map.of(
                    "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt))))
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            ResponseEntity<Map> response = restTemplate.exchange(
                    geminiUrl, HttpMethod.POST,
                    new HttpEntity<>(body, headers), Map.class
            );

            List<Map> candidates = (List<Map>) response.getBody().get("candidates");
            Map content = (Map) candidates.get(0).get("content");
            List<Map> parts = (List<Map>) content.get("parts");
            String translated = ((String) parts.get(0).get("text")).trim();

            System.out.println("✅ Gemini OK: " + translated);
            return buildResponse(translated, detectedLang, langInfo, text);

        } catch (Exception e1) {
            System.err.println("⚠️ Gemini failed: " + e1.getMessage());
        }

        // ── 2. Fallback MyMemory (sans URLDecoder) ────────────────
        try {
            String url = UriComponentsBuilder
                    .fromHttpUrl("https://api.mymemory.translated.net/get")
                    .queryParam("q", text)
                    .queryParam("langpair", sourceCode + "|" + targetCode)
                    .build().toUriString();

            ResponseEntity<Map> resp = restTemplate.getForEntity(url, Map.class);
            Map data = (Map) resp.getBody().get("responseData");
            String translated = (String) data.get("translatedText");

            // Vérifier que ce n'est pas du texte encodé %XX
            if (translated != null && !translated.contains("%")
                    && !translated.isBlank()) {
                System.out.println("✅ MyMemory OK: " + translated);
                return buildResponse(translated, detectedLang, langInfo, text);
            }
            System.err.println("⚠️ MyMemory retourné encodé, essai suivant");

        } catch (Exception e2) {
            System.err.println("⚠️ MyMemory failed: " + e2.getMessage());
        }

        // ── 3. Fallback LibreTranslate (gratuit, fiable) ──────────
        try {
            String libreUrl = "https://libretranslate.com/translate";

            Map<String, String> body = Map.of(
                    "q",      text,
                    "source", sourceCode,
                    "target", targetCode,
                    "format", "text"
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            ResponseEntity<Map> resp = restTemplate.exchange(
                    libreUrl, HttpMethod.POST,
                    new HttpEntity<>(body, headers), Map.class
            );

            String translated = (String) resp.getBody().get("translatedText");
            if (translated != null && !translated.isBlank()) {
                System.out.println("✅ LibreTranslate OK: " + translated);
                return buildResponse(translated, detectedLang, langInfo, text);
            }

        } catch (Exception e3) {
            System.err.println("⚠️ LibreTranslate failed: " + e3.getMessage());
        }

        // ── 4. Fallback Google Translate non-officiel ─────────────
        try {
            String googleUrl = UriComponentsBuilder
                    .fromHttpUrl("https://translate.googleapis.com/translate_a/single")
                    .queryParam("client", "gtx")
                    .queryParam("sl", sourceCode)
                    .queryParam("tl", targetCode)
                    .queryParam("dt", "t")
                    .queryParam("q", text)
                    .build().toUriString();

            ResponseEntity<String> resp = restTemplate.getForEntity(
                    googleUrl, String.class
            );

            // Parser la réponse JSON brute de Google
            String raw = resp.getBody();
            if (raw != null && raw.startsWith("[[")) {
                // Format: [[ ["traduction","original",...], ...], ...]
                String translated = extractGoogleTranslation(raw);
                if (!translated.isBlank()) {
                    System.out.println("✅ Google Translate OK: " + translated);
                    return buildResponse(translated, detectedLang, langInfo, text);
                }
            }

        } catch (Exception e4) {
            System.err.println("⚠️ Google Translate failed: " + e4.getMessage());
        }

        // ── 5. Dernier recours : retourner le texte original ──────
        return ResponseEntity.ok(Map.of(
                "translatedText",   text,
                "detectedLanguage", detectedLang,
                "languageName",     langInfo[0],
                "languageFlag",     langInfo[1],
                "originalText",     text,
                "isFrench",         "FR".equals(detectedLang),
                "error",            "Traduction temporairement indisponible"
        ));
    }

    // ── Helpers ──────────────────────────────────────────────────

    private ResponseEntity<?> buildResponse(
            String translated, String detectedLang,
            String[] langInfo, String originalText) {
        return ResponseEntity.ok(Map.of(
                "translatedText",   translated,
                "detectedLanguage", detectedLang,
                "languageName",     langInfo[0],
                "languageFlag",     langInfo[1],
                "originalText",     originalText,
                "isFrench",         "FR".equals(detectedLang)
        ));
    }

    private String extractGoogleTranslation(String raw) {
        StringBuilder result = new StringBuilder();
        try {
            // Format brut: [[["translation","original",null,null,10],...],...]
            int i = 3;
            while (i < raw.length()) {
                int start = raw.indexOf("\"", i);
                if (start == -1) break;
                int end = raw.indexOf("\"", start + 1);
                if (end == -1) break;
                String part = raw.substring(start + 1, end);
                if (!part.isEmpty() && !part.equals("null")) {
                    result.append(part);
                }
                // Vérifier si on est à la fin du premier segment
                i = end + 1;
                if (raw.charAt(i) == ',') {
                    // Suivant
                    int nextBracket = raw.indexOf("[", i);
                    if (nextBracket == -1 || raw.charAt(nextBracket + 1) == '[') break;
                }
                break;
            }
        } catch (Exception e) {
            System.err.println("Parsing Google failed: " + e.getMessage());
        }
        return result.toString();
    }

    private String detectLang(String text) {
        if (text == null || text.trim().isEmpty()) return "FR";
        if (text.matches(".*[\\u0600-\\u06FF].*")) return "AR";
        if (text.matches(".*[\\u0400-\\u04FF].*")) return "RU";
        if (text.matches(".*[\\u4E00-\\u9FFF].*")) return "ZH";
        String lower = text.toLowerCase();
        if (lower.matches(".*(bonjour|merci|j'ai|vous |une |les |des |est |avec|pour|dans|problème|paiement|cours|erreur|certificat).*")) return "FR";
        if (lower.matches(".*(hello|thank|please|the |this |have|your |payment|problem|error|course|certificate).*")) return "EN";
        if (lower.matches(".*(hola|gracias|tengo|problema|pago|certificado|curso).*")) return "ES";
        if (lower.matches(".*(hallo|danke|bitte|zahlung|problem|fehler|kurs).*")) return "DE";
        if (lower.matches(".*(ciao|grazie|problema|pagamento|errore|corso).*")) return "IT";
        return "FR";
    }

    private String getLangIsoCode(String langCode) {
        return switch (langCode) {
            case "AR" -> "ar";
            case "EN" -> "en";
            case "FR" -> "fr";
            case "ES" -> "es";
            case "DE" -> "de";
            case "IT" -> "it";
            case "RU" -> "ru";
            case "ZH" -> "zh";
            default   -> "auto";
        };
    }

    private String getTargetIsoCode(String targetLang) {
        return switch (targetLang.toLowerCase().trim()) {
            case "arabe"    -> "ar";
            case "anglais"  -> "en";
            case "français" -> "fr";
            case "espagnol" -> "es";
            case "allemand" -> "de";
            case "italien"  -> "it";
            case "russe"    -> "ru";
            case "chinois"  -> "zh";
            default         -> "fr";
        };
    }

    private String[] getLangInfo(String lang) {
        return switch (lang) {
            case "AR" -> new String[]{"Arabe",    "🇸🇦"};
            case "EN" -> new String[]{"Anglais",  "🇬🇧"};
            case "DE" -> new String[]{"Allemand", "🇩🇪"};
            case "ES" -> new String[]{"Espagnol", "🇪🇸"};
            case "IT" -> new String[]{"Italien",  "🇮🇹"};
            case "RU" -> new String[]{"Russe",    "🇷🇺"};
            case "ZH" -> new String[]{"Chinois",  "🇨🇳"};
            default   -> new String[]{"Français", "🇫🇷"};
        };
    }
}