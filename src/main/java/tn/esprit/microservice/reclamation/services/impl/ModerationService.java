package tn.esprit.microservice.reclamation.services.impl;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tn.esprit.microservice.reclamation.entities.ModerationResult;
import tn.esprit.microservice.reclamation.entities.Reclamation;
import tn.esprit.microservice.reclamation.services.interfaces.IModerationService;

import java.util.*;

@Service
public class ModerationService implements IModerationService {

    @Value("${app.banned.words}")
    private String bannedWordsConfig;

    private List<String> bannedWords;

    @PostConstruct
    public void init() {
        bannedWords = Arrays.asList(bannedWordsConfig.split(","));
    }

    @Override
    public ModerationResult scanReclamation(Reclamation reclamation) {
        String text = (reclamation.getSubject() + " " + reclamation.getDescription()).toLowerCase();

        List<String> detectedWords = new ArrayList<>();

        for (String word : bannedWords) {
            if (text.contains(word.toLowerCase().trim())) {
                detectedWords.add(word);
            }
        }

        if (!detectedWords.isEmpty()) {
            return ModerationResult.builder()
                    .isSuspect(true)
                    .detectedWords(detectedWords)
                    .reason("Mots interdits détectés: " + String.join(", ", detectedWords))
                    .build();
        }

        return ModerationResult.builder()
                .isSuspect(false)
                .build();
    }
}