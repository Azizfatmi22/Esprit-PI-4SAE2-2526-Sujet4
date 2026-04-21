package tn.esprit.microservice.reclamation.services.impl;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import tn.esprit.microservice.reclamation.entities.*;
import tn.esprit.microservice.reclamation.repositories.ReclamationRepository;
import tn.esprit.microservice.reclamation.services.interfaces.IGravityCalculationService;
import tn.esprit.microservice.reclamation.services.interfaces.IGravityCalculationService.GravityLevel;
import tn.esprit.microservice.reclamation.services.interfaces.IGravityCalculationService.GravityResult;
import tn.esprit.microservice.reclamation.services.interfaces.IModerationService;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReclamationServiceImplTest {

    @Mock
    private ReclamationRepository reclamationRepository;

    @Mock
    private IModerationService moderationService;

    @Mock
    private IGravityCalculationService gravityService;

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private ReclamationServiceImpl reclamationService;

    private Reclamation reclamation;

    @BeforeEach
    void setUp() {
        reclamation = Reclamation.builder()
                .id(1L)
                .learnerId("100")
                .subject("Test subject")
                .description("Test description")
                .type(ReclamationType.TECHNICAL)
                .status(ReclamationStatus.PENDING)
                .priority(2)
                .build();
    }

    // =========================================================
    // CREATE RECLAMATION
    // =========================================================
    @Test
    void testCreateReclamation_success() {

        ModerationResult moderationResult = new ModerationResult();
        moderationResult.setSuspect(false);

        when(moderationService.scanReclamation(any()))
                .thenReturn(moderationResult);

        when(gravityService.calculateGravity(any()))
                .thenReturn(new GravityResult(5, GravityLevel.MEDIUM));

        when(reclamationRepository.save(any(Reclamation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Reclamation result = reclamationService.createReclamation(reclamation);

        assertNotNull(result);
        assertEquals(ReclamationStatus.PENDING, result.getStatus());
        assertEquals(5, result.getGravityScore());
        assertEquals("MEDIUM", result.getGravityLevel());
    }

    // =========================================================
    // GET BY ID
    // =========================================================
    @Test
    void testGetReclamationById_found() {

        when(reclamationRepository.findById(1L))
                .thenReturn(Optional.of(reclamation));

        Reclamation result = reclamationService.getReclamationById(1L);

        assertNotNull(result);
        assertEquals("100", result.getLearnerId());
    }

    @Test
    void testGetReclamationById_notFound() {

        when(reclamationRepository.findById(1L))
                .thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> reclamationService.getReclamationById(1L));

        assertTrue(ex.getMessage().contains("non trouvée"));
    }

    // =========================================================
    // UPDATE STATUS
    // =========================================================
    @Test
    void testUpdateReclamationStatus() {

        when(reclamationRepository.findById(1L))
                .thenReturn(Optional.of(reclamation));

        when(reclamationRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Reclamation result = reclamationService.updateReclamationStatus(
                1L,
                ReclamationStatus.RESOLVED,
                10L
        );

        assertEquals(ReclamationStatus.RESOLVED, result.getStatus());
        assertEquals(10L, result.getAdminId());
        assertNotNull(result.getResolvedDate());
    }

    // =========================================================
    // SATISFACTION
    // =========================================================
    @Test
    void testSubmitSatisfaction() {

        when(reclamationRepository.findById(1L))
                .thenReturn(Optional.of(reclamation));

        when(reclamationRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Reclamation result = reclamationService.submitSatisfaction(
                1L,
                5,
                "Très satisfait"
        );

        assertEquals(5, result.getSatisfactionScore());
        assertEquals("Très satisfait", result.getSatisfactionComment());
        assertNotNull(result.getSatisfactionDate());
    }

    // =========================================================
    // DELETE
    // =========================================================
    @Test
    void testDeleteReclamation() {

        when(reclamationRepository.findById(1L))
                .thenReturn(Optional.of(reclamation));

        reclamationService.deleteReclamation(1L);

        verify(reclamationRepository).delete(reclamation);
    }
}