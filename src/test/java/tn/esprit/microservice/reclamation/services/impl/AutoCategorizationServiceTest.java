package tn.esprit.microservice.reclamation.services.impl;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tn.esprit.microservice.reclamation.entities.ReclamationType;
import tn.esprit.microservice.reclamation.services.impl.AutoCategorizationService.CategorizationResult;

import static org.junit.jupiter.api.Assertions.*;

class AutoCategorizationServiceTest {

    private AutoCategorizationService service;

    @BeforeEach
    void setUp() {
        service = new AutoCategorizationService();
    }

    @Test
    void testDetectTechnical() {
        CategorizationResult result =
                service.detectType("Application bug", "la page crash avec erreur 500");

        assertEquals(ReclamationType.TECHNICAL, result.getSuggestedType());
        assertTrue(result.getConfidence() > 0);
        assertTrue(result.isHasSuggestion());
        assertFalse(result.getMatchedKeywords().isEmpty());
    }

    @Test
    void testDetectPayment() {
        CategorizationResult result =
                service.detectType("Paiement refusé", "ma carte bancaire ne marche pas");

        assertEquals(ReclamationType.PAYMENT, result.getSuggestedType());
        assertTrue(result.isHasSuggestion());
    }

    @Test
    void testDetectAccess() {
        CategorizationResult result =
                service.detectType("Connexion impossible", "mot de passe oublié");

        assertEquals(ReclamationType.ACCESS, result.getSuggestedType());
    }

    @Test
    void testDetectOther() {
        CategorizationResult result =
                service.detectType("Bonjour", "question générale");

        assertEquals(ReclamationType.OTHER, result.getSuggestedType());
        assertEquals(0, result.getConfidence());
        assertFalse(result.isHasSuggestion());
    }

    @Test
    void testGetTypeLabel() {
        assertEquals("Problème technique",
                service.getTypeLabel(ReclamationType.TECHNICAL));

        assertEquals("Problème de paiement",
                service.getTypeLabel(ReclamationType.PAYMENT));

        assertEquals("Autre",
                service.getTypeLabel(ReclamationType.OTHER));
    }
}