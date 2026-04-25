package tn.esprit.microservice.reclamation.services.impl;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tn.esprit.microservice.reclamation.entities.Reclamation;
import tn.esprit.microservice.reclamation.entities.ReclamationType;
import tn.esprit.microservice.reclamation.services.interfaces.IGravityCalculationService.GravityLevel;
import tn.esprit.microservice.reclamation.services.interfaces.IGravityCalculationService.GravityResult;

import static org.junit.jupiter.api.Assertions.*;

class GravityCalculationServiceTest {

    private GravityCalculationService service;

    @BeforeEach
    void setUp() {
        service = new GravityCalculationService();
    }

    @Test
    void testCriticalCase() {

        Reclamation r = new Reclamation();
        r.setSubject("URGENT blocé system");
        r.setDescription("total payment issue important");
        r.setType(ReclamationType.PAYMENT);
        r.setAmount(200.0);
        r.setPriority(1);

        GravityResult result = service.calculateGravity(r);

        assertTrue(result.getScore() >= 7);
        assertEquals(GravityLevel.CRITICAL, result.getLevel());
    }

    @Test
    void testHighCase() {

        Reclamation r = new Reclamation();
        r.setSubject("important issue");
        r.setDescription("system message");
        r.setType(ReclamationType.TECHNICAL);
        r.setPriority(2);

        GravityResult result = service.calculateGravity(r);

        assertEquals(GravityLevel.HIGH, result.getLevel());
    }

    @Test
    void testMediumCase() {

        Reclamation r = new Reclamation();
        r.setSubject("normal request");
        r.setDescription("urgent but not blocked");
        r.setType(ReclamationType.ACCESS);
        r.setPriority(2);

        GravityResult result = service.calculateGravity(r);

        assertTrue(result.getScore() >= 3 && result.getScore() < 5);
        assertEquals(GravityLevel.MEDIUM, result.getLevel());
    }

    @Test
    void testLowCase() {

        Reclamation r = new Reclamation();
        r.setSubject("hello");
        r.setDescription("simple question");
        r.setType(ReclamationType.OTHER);
        r.setPriority(3);

        GravityResult result = service.calculateGravity(r);

        assertTrue(result.getScore() < 3);
        assertEquals(GravityLevel.LOW, result.getLevel());
    }
}