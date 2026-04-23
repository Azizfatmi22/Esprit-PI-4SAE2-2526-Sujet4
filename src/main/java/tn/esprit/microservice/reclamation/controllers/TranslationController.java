package tn.esprit.microservice.reclamation.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger logger = LoggerFactory.getLogger(TranslationController.class);

    private static final String TRANSLATED_TEXT   = "translatedText";
    private static final String DETECTED_LANGUAGE = "detectedLanguage";
    private static final String LANGUAGE_NAME     = "languageName";
    private static final String LANGUAGE_FLAG     = "languageFlag";
    private static final String ORIGINAL_TEXT     = "originalText";
    private static final String IS_FRENCH         = "isFrench";
    private static final String ERROR_KEY         = "error";

    @Autowired
    private RestTemplate restTemplate;

    @Value("${anthropic.api.key}")
    private String geminiApiKey;

    @PostMapping("/detect")
    public ResponseEntity<Object> detectLanguage(@RequestBody Map<String, String> payload) {
        String text = payload.getOrDefault("text", "").trim();
        String lang = detectLang(text);
        String[] info = getLangInfo(lang);
        return ResponseEntity.ok(Map.of(
                DETECTED_LANGUAGE, lang,
                LANGUAGE_NAME,     info[0],
                LANGUAGE_FLAG,     info[1],
                IS_FRENCH,         "FR".equals(lang)
        ));
    }

    @PostMapping("/translate")
    public ResponseEntity<Object> translate(@RequestBody Map<String, String> payload) {
        String text       = payload.getOrDefault("text", "").trim();
        String targetLang = payload.getOrDefault("targetLang", "français");

        if (text.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(ERROR_KEY, "Texte vide"));
        }

        String detectedLang = detectLang(text);
        String[] langInfo   = getLangInfo(detectedLang);
        String sourceCode   = getLangIsoCode(detectedLang);
        String targetCode   = getTargetIsoCode(targetLang);

        // 1. Essayer Gemini
        ResponseEntity<Object> geminiResult = tryGemini(text, targetLang, detectedLang, langInfo);
        if (geminiResult != null) return geminiResult;

        // 2. Fallback MyMemory
        ResponseEntity<Object> myMemoryResult = tryMyMemory(text, sourceCode, targetCode, detectedLang, langInfo);
        if (myMemoryResult != null) return myMemoryResult;

        // 3. Fallback LibreTranslate
        ResponseEntity<Object> libreResult = tryLibreTranslate(text, sourceCode, targetCode, detectedLang, langInfo);
        if (libreResult != null) return libreResult;

        // 4. Fallback Google Translate
        ResponseEntity<Object> googleResult = tryGoogle(text, sourceCode, targetCode, detectedLang, langInfo);
        if (googleResult != null) return googleResult;

        // 5. Dernier recours
        return ResponseEntity.ok(Map.of(
                TRANSLATED_TEXT,   text,
                DETECTED_LANGUAGE, detectedLang,
                LANGUAGE_NAME,     langInfo[0],
                LANGUAGE_FLAG,     langInfo[1],
                ORIGINAL_TEXT,     text,
                IS_FRENCH,         "FR".equals(detectedLang),
                ERROR_KEY,         "Traduction temporairement indisponible"
        ));
    }

    private ResponseEntity<Object> tryGemini(String text, String targetLang,
                                             String detectedLang, String[] langInfo) {
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

            if (response.getBody() == null) return null;

            List<Map> candidates = (List<Map>) response.getBody().get("candidates");
            Map content = (Map) candidates.get(0).get("content");
            List<Map> parts = (List<Map>) content.get("parts");
            String translated = ((String) parts.get(0).get("text")).trim();

            logger.info("Gemini OK: {}", translated);
            return buildResponse(translated, detectedLang, langInfo, text);

        } catch (Exception e) {
            logger.warn("Gemini failed: {}", e.getMessage());
            return null;
        }
    }

    private ResponseEntity<Object> tryMyMemory(String text, String sourceCode,
                                               String targetCode, String detectedLang,
                                               String[] langInfo) {
        try {
            String url = UriComponentsBuilder
                    .fromHttpUrl("https://api.mymemory.translated.net/get")
                    .queryParam("q", text)
                    .queryParam("langpair", sourceCode + "|" + targetCode)
                    .build().toUriString();

            ResponseEntity<Map> resp = restTemplate.getForEntity(url, Map.class);

            if (resp.getBody() == null) return null;

            Map data = (Map) resp.getBody().get("responseData");
            if (data == null) return null;

            String translated = (String) data.get("translatedText");

            if (translated != null && !translated.contains("%") && !translated.isBlank()) {
                logger.info("MyMemory OK: {}", translated);
                return buildResponse(translated, detectedLang, langInfo, text);
            }
        } catch (Exception e) {
            logger.warn("MyMemory failed: {}", e.getMessage());
        }
        return null;
    }

    private ResponseEntity<Object> tryLibreTranslate(String text, String sourceCode,
                                                     String targetCode, String detectedLang,
                                                     String[] langInfo) {
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

            if (resp.getBody() == null) return null;

            String translated = (String) resp.getBody().get("translatedText");
            if (translated != null && !translated.isBlank()) {
                logger.info("LibreTranslate OK: {}", translated);
                return buildResponse(translated, detectedLang, langInfo, text);
            }
        } catch (Exception e) {
            logger.warn("LibreTranslate failed: {}", e.getMessage());
        }
        return null;
    }

    private ResponseEntity<Object> tryGoogle(String text, String sourceCode,
                                             String targetCode, String detectedLang,
                                             String[] langInfo) {
        try {
            String googleUrl = UriComponentsBuilder
                    .fromHttpUrl("https://translate.googleapis.com/translate_a/single")
                    .queryParam("client", "gtx")
                    .queryParam("sl", sourceCode)
                    .queryParam("tl", targetCode)
                    .queryParam("dt", "t")
                    .queryParam("q", text)
                    .build().toUriString();

            ResponseEntity<String> resp = restTemplate.getForEntity(googleUrl, String.class);
            String raw = resp.getBody();

            if (raw != null && raw.startsWith("[[")) {
                String translated = extractGoogleTranslation(raw);
                if (!translated.isBlank()) {
                    logger.info("Google Translate OK: {}", translated);
                    return buildResponse(translated, detectedLang, langInfo, text);
                }
            }
        } catch (Exception e) {
            logger.warn("Google Translate failed: {}", e.getMessage());
        }
        return null;
    }

    private ResponseEntity<Object> buildResponse(String translated, String detectedLang,
                                                 String[] langInfo, String originalText) {
        return ResponseEntity.ok(Map.of(
                TRANSLATED_TEXT,   translated,
                DETECTED_LANGUAGE, detectedLang,
                LANGUAGE_NAME,     langInfo[0],
                LANGUAGE_FLAG,     langInfo[1],
                ORIGINAL_TEXT,     originalText,
                IS_FRENCH,         "FR".equals(detectedLang)
        ));
    }

    private String extractGoogleTranslation(String raw) {
        StringBuilder result = new StringBuilder();
        try {
            int i = 3;
            while (i < raw.length()) {
                int start = raw.indexOf("\"", i);
                if (start == -1) break;
                int end = raw.indexOf("\"", start + 1);
                if (end == -1) break;
                String part = raw.substring(start + 1, end);
                if (!part.isEmpty() && !"null".equals(part)) {
                    result.append(part);
                }
                i = end + 1;
                if (i < raw.length() && raw.charAt(i) == ',') {
                    int nextBracket = raw.indexOf("[", i);
                    if (nextBracket == -1 || raw.charAt(nextBracket + 1) == '[') break;
                }
                break;
            }
        } catch (Exception e) {
            logger.warn("Parsing Google failed: {}", e.getMessage());
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