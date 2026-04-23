package tn.esprit.microservice.reclamation.services.impl;


import org.springframework.stereotype.Service;
import tn.esprit.microservice.reclamation.entities.ReclamationType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class AutoCategorizationService {

    // Définition des mots-clés par type de réclamation
    private final Map<ReclamationType, List<String>> keywords = new HashMap<>();

    public AutoCategorizationService() {
        // Mots-clés pour les problèmes techniques
        keywords.put(ReclamationType.TECHNICAL, List.of(
                "bug", "erreur", "plante", "crash", "beug", "lent", "ralenti",
                "502", "404", "500", "erreur serveur", "page blanche", "ne charge pas",
                "ne s'affiche pas", "video ne marche pas", "son", "audio"
        ));

        // Mots-clés pour les problèmes de paiement
        keywords.put(ReclamationType.PAYMENT, List.of(
                "paiement", "carte", "cb", "carte bancaire", "remboursement",
                "facture", "transaction", "paypal", "virement", "débit", "crédit",
                "non remboursé", "facturé deux fois", "montant", "prix"
        ));

        // Mots-clés pour les problèmes de contenu
        keywords.put(ReclamationType.CONTENT, List.of(
                "cours", "leçon", "module", "chapitre", "video", "quiz", "exercice",
                "examen", "contenu", "pdf", "document", "erreur de contenu",
                "fausse réponse", "mauvaise réponse", "information erronée"
        ));

        // Mots-clés pour les problèmes d'accès
        keywords.put(ReclamationType.ACCESS, List.of(
                "connexion", "login", "mot de passe", "accès", "bloqué", "verrouillé",
                "compte", "session", "expiré", "refusé", "interdit", "plus accès",
                "impossible de se connecter", "oublié"
        ));

        // Mots-clés pour les problèmes de certificat
        keywords.put(ReclamationType.CERTIFICATE, List.of(
                "certificat", "attestation", "diplôme", "validation", "complétion",
                "certificat manquant", "certificat erroné", "fini le cours",
                "termine le cours", "finalisé", "ne reçois pas mon certificat"
        ));
    }

    /**
     * Détecte le type de réclamation basé sur le texte
     */
    public CategorizationResult detectType(String subject, String description) {
        String text = (subject + " " + description).toLowerCase();

        Map<ReclamationType, Integer> scores = new HashMap<>();

        // Calculer le score pour chaque type
        for (Map.Entry<ReclamationType, List<String>> entry : keywords.entrySet()) {
            int score = 0;
            for (String keyword : entry.getValue()) {
                if (text.contains(keyword)) {
                    score++;
                    // Bonus pour mots-clés plus spécifiques
                    if (keyword.length() > 8) {
                        score++;
                    }
                }
            }
            if (score > 0) {
                scores.put(entry.getKey(), score);
            }
        }

        // Trouver le type avec le meilleur score
        if (scores.isEmpty()) {
            return CategorizationResult.builder()
                    .suggestedType(ReclamationType.OTHER)
                    .confidence(0)
                    .hasSuggestion(false)
                    .build();
        }

        Optional<Map.Entry<ReclamationType, Integer>> maxEntry = scores.entrySet().stream()
                .max(Map.Entry.comparingByValue());

        if (maxEntry.isEmpty()) {
            return CategorizationResult.builder()
                    .suggestedType(ReclamationType.OTHER)
                    .confidence(0)
                    .hasSuggestion(false)
                    .build();
        }

        ReclamationType bestType = maxEntry.get().getKey();

        int bestScore = scores.get(bestType);
        int confidence = Math.min(100, (bestScore * 100) / 5); // Max 100%

        return CategorizationResult.builder()
                .suggestedType(bestType)
                .confidence(confidence)
                .hasSuggestion(confidence >= 30)
                .matchedKeywords(getMatchedKeywords(text, bestType))
                .build();
    }

    private List<String> getMatchedKeywords(String text, ReclamationType type) {
        return keywords.get(type).stream()
                .filter(text::contains)
                .limit(3)
                .toList();
    }

    /**
     * Retourne le libellé du type pour affichage
     */
    public String getTypeLabel(ReclamationType type) {
        return switch (type) {
            case TECHNICAL -> "Problème technique";
            case PAYMENT -> "Problème de paiement";
            case CONTENT -> "Problème de contenu";
            case ACCESS -> "Problème d'accès";
            case CERTIFICATE -> "Problème de certificat";
            case OTHER -> "Autre";
        };
    }

    /**
     * Résultat de la catégorisation
     */
    public static class CategorizationResult {
        private ReclamationType suggestedType;
        private int confidence;
        private boolean hasSuggestion;
        private List<String> matchedKeywords;

        // Constructeurs, getters, setters
        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private ReclamationType suggestedType;
            private int confidence;
            private boolean hasSuggestion;
            private List<String> matchedKeywords;

            public Builder suggestedType(ReclamationType type) {
                this.suggestedType = type;
                return this;
            }

            public Builder confidence(int confidence) {
                this.confidence = confidence;
                return this;
            }

            public Builder hasSuggestion(boolean hasSuggestion) {
                this.hasSuggestion = hasSuggestion;
                return this;
            }

            public Builder matchedKeywords(List<String> keywords) {
                this.matchedKeywords = keywords;
                return this;
            }

            public CategorizationResult build() {
                CategorizationResult result = new CategorizationResult();
                result.suggestedType = this.suggestedType;
                result.confidence = this.confidence;
                result.hasSuggestion = this.hasSuggestion;
                result.matchedKeywords = this.matchedKeywords;
                return result;
            }
        }

        // Getters
        public ReclamationType getSuggestedType() { return suggestedType; }
        public int getConfidence() { return confidence; }
        public boolean isHasSuggestion() { return hasSuggestion; }
        public List<String> getMatchedKeywords() { return matchedKeywords; }
    }
}