package com.example.mstrainerhiring.services;

import com.example.mstrainerhiring.entities.TrainerHiring;
import java.io.File;
import java.util.UUID;

public interface IntelligenceService {
    /**
     * Extracts text from a PDF file.
     */
    String extractTextFromPdf(String filePath);

    /**
     * Performs a complete intelligence analysis on a trainer application.
     */
    TrainerHiring analyzeApplication(TrainerHiring application, String cvText);

    /**
     * Calculates similarity between two strings using Levenshtein distance.
     */
    double calculateSimilarity(String s1, String s2);
}
