package tn.esprit.microservice.reclamation.services.impl;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tn.esprit.microservice.reclamation.entities.ModerationResult;
import tn.esprit.microservice.reclamation.entities.Reclamation;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

class ModerationServiceTest {

    private ModerationService moderationService;

    @BeforeEach
    void setUp() throws Exception {
        moderationService = new ModerationService();

        // injecter banned words manuellement (simulate @Value)
        Field field = ModerationService.class.getDeclaredField("bannedWordsConfig");
        field.setAccessible(true);
        field.set(moderationService, "spam,abuse,badword");

        moderationService.init();
    }

    @Test
    void testScanReclamation_ShouldDetectBannedWords() {

        Reclamation reclamation = new Reclamation();
        reclamation.setSubject("This is spam message");
        reclamation.setDescription("contains abuse content");

        ModerationResult result = moderationService.scanReclamation(reclamation);

        assertTrue(result.isSuspect());
        assertEquals(2, result.getDetectedWords().size());
        assertTrue(result.getReason().contains("spam"));
    }

    @Test
    void testScanReclamation_ShouldBeClean() {

        Reclamation reclamation = new Reclamation();
        reclamation.setSubject("Hello world");
        reclamation.setDescription("This is a normal message");

        ModerationResult result = moderationService.scanReclamation(reclamation);

        assertFalse(result.isSuspect());
        assertNull(result.getDetectedWords());
    }
}