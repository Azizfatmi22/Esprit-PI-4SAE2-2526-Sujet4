package tn.esprit.microservice.reclamation.services.impl;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import tn.esprit.microservice.reclamation.config.SlaConfig;
import tn.esprit.microservice.reclamation.entities.Reclamation;
import tn.esprit.microservice.reclamation.entities.ReclamationStatus;
import tn.esprit.microservice.reclamation.entities.SlaAlert;
import tn.esprit.microservice.reclamation.repositories.SlaAlertRepository;
import tn.esprit.microservice.reclamation.services.impl.SlaService;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SlaServiceTest {

    @Mock
    private SlaAlertRepository slaAlertRepository;

    @Mock
    private JavaMailSender mailSender;

    private SlaConfig slaConfig; // VRAI OBJET

    private SlaService slaService;

    private Reclamation reclamation;

    @BeforeEach
    void setUp() {

        slaConfig = new SlaConfig();

        slaService = new SlaService(
                slaAlertRepository,
                mailSender,
                slaConfig
        );

        reclamation = new Reclamation();
        reclamation.setId(1L);
        reclamation.setPriority(1);
        reclamation.setStatus(ReclamationStatus.IN_PROGRESS);
        reclamation.setCreatedDate(LocalDateTime.now().minusMinutes(20));
        reclamation.setSubject("Test");
        reclamation.setLearnerId("5");
    }

    @Test
    void testIsSlaBreached_ReturnTrue() {
        boolean result = slaService.isSlaBreached(reclamation);
        assertTrue(result);
    }

    @Test
    void testIsSlaBreached_ReturnFalse_WhenResolved() {
        reclamation.setStatus(ReclamationStatus.RESOLVED);

        boolean result = slaService.isSlaBreached(reclamation);

        assertFalse(result);
    }

    @Test
    void testGetElapsedMinutes() {
        long minutes = slaService.getElapsedMinutes(reclamation);
        assertTrue(minutes >= 20);
    }

    @Test
    void testCreateAlertIfNotExists() {

        when(slaAlertRepository.findByReclamationIdAndResolvedFalse(1L))
                .thenReturn(Optional.empty());

        when(slaAlertRepository.save(any(SlaAlert.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        slaService.createAlertIfNotExists(reclamation);

        verify(slaAlertRepository, atLeastOnce()).save(any());
        verify(mailSender, times(1))
                .send(any(org.springframework.mail.SimpleMailMessage.class));
    }

    @Test
    void testResolveAlert() {

        SlaAlert alert = new SlaAlert();
        alert.setResolved(false);

        when(slaAlertRepository.findByReclamationIdAndResolvedFalse(1L))
                .thenReturn(Optional.of(alert));

        slaService.resolveAlert(1L);

        assertTrue(alert.getResolved());
        verify(slaAlertRepository).save(alert);
    }

    @Test
    void testGetActiveAlerts() {

        when(slaAlertRepository.findByResolvedFalseOrderByAlertDateDesc())
                .thenReturn(List.of(new SlaAlert()));

        List<SlaAlert> result = slaService.getActiveAlerts();

        assertEquals(1, result.size());
    }
}