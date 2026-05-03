package tn.esprit.microservice.reclamation.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import tn.esprit.microservice.reclamation.entities.Reclamation;
import tn.esprit.microservice.reclamation.entities.ReclamationType;
import tn.esprit.microservice.reclamation.services.interfaces.IReclamationService;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AiSuggestionControllerTest {

    private AiSuggestionController controller;
    private IReclamationService reclamationService;

    @BeforeEach
    void setUp() {

        controller = new AiSuggestionController();

        reclamationService = mock(IReclamationService.class);

        RestTemplate restTemplate = new RestTemplate();

        ReflectionTestUtils.setField(controller, "reclamationService", reclamationService);
        ReflectionTestUtils.setField(controller, "restTemplate", restTemplate);
        ReflectionTestUtils.setField(controller, "apiKey", "fake-key");
    }

    @Test
    void testSuggestResponse_ReclamationNotFound() {

        when(reclamationService.getReclamationById(99L))
                .thenThrow(new RuntimeException("Not found"));

        ResponseEntity<?> response = controller.suggestResponse(99L);

        assertEquals(500, response.getStatusCodeValue());
    }

    @Test
    void testSuggestResponse_LocalFallback() {

        Reclamation r = new Reclamation();
        r.setId(1L);
        r.setType(ReclamationType.PAYMENT);
        r.setSubject("Paiement double");
        r.setDescription("Débit 150 DT");
        r.setPriority(1);

        when(reclamationService.getReclamationById(1L))
                .thenReturn(r);

        when(reclamationService.getReclamationsByStatus(any()))
                .thenReturn(List.of());

        ResponseEntity<?> response = controller.suggestResponse(1L);

        assertEquals(200, response.getStatusCodeValue());

        Map<?, ?> body = (Map<?, ?>) response.getBody();
        List<?> suggestions = (List<?>) body.get("suggestions");

        assertEquals(3, suggestions.size());
    }
}