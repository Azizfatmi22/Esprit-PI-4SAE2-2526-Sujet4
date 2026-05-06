package com.sessionmanagementservice.services;



import com.sessionmanagementservice.Repositories.PlanningRepository;
import com.sessionmanagementservice.Repositories.SessionRepository;
import com.sessionmanagementservice.Services.impl.SessionServiceImpl;
import com.sessionmanagementservice.entities.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SessionServiceImplTest {

    @Mock
    private SessionRepository sessionRepository;

    @Mock
    private PlanningRepository planningRepository;

    @Mock
    private Jwt jwt;

    @InjectMocks
    private SessionServiceImpl service;

    private Session session;

    @BeforeEach
    void setUp() {
        session = new Session();
        session.setId(1L);
        session.setCreatedAt(LocalDate.now());
        session.setMaxParticipants(10);
        session.setStatus(SessionStatus.PLANNED);
    }

    // =========================
    // CREATE SESSION
    // =========================

    @Test
    void shouldCreateSessionSuccessfully() {
        when(jwt.getSubject()).thenReturn("trainer1");
        when(sessionRepository.findByTrainerIdAndCreatedAt(any(), any()))
                .thenReturn(List.of());
        when(sessionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Session result = service.createSession(session, jwt);

        assertNotNull(result);
        assertEquals("trainer1", result.getTrainerId());
        assertEquals(SessionStatus.PLANNED, result.getStatus());
    }

    @Test
    void shouldThrowWhenTrainerOverloaded() {
        when(jwt.getSubject()).thenReturn("trainer1");

        when(sessionRepository.findByTrainerIdAndCreatedAt(any(), any()))
                .thenReturn(List.of(new Session(), new Session(), new Session()));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.createSession(session, jwt));

        assertTrue(ex.getMessage().contains("surchargé"));
    }

    // =========================
    // UPDATE SESSION
    // =========================

    @Test
    void shouldUpdateSessionSuccessfully() {
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(sessionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Session update = new Session();
        update.setMaxParticipants(20);

        Session result = service.updateSession(1L, update);

        assertEquals(20, result.getMaxParticipants());
    }

    @Test
    void shouldThrowWhenUpdatingCompletedSession() {
        session.setStatus(SessionStatus.COMPLETED);

        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));

        Session update = new Session();

        assertThrows(RuntimeException.class,
                () -> service.updateSession(1L, update));
    }

    // =========================
    // DELETE SESSION
    // =========================

    @Test
    void shouldDeleteSession() {
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));

        service.deleteSession(1L);

        verify(sessionRepository).delete(session);
    }

    @Test
    void shouldNotDeleteOngoingSession() {
        session.setStatus(SessionStatus.ONGOING);
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));

        assertThrows(RuntimeException.class,
                () -> service.deleteSession(1L));
    }

    // =========================
    // DATE VALIDATION
    // =========================

    @Test
    void shouldValidateDateCorrectly() {
        assertTrue(service.isDateValid(LocalDate.now()));
        assertFalse(service.isDateValid(LocalDate.now().minusDays(1)));
    }

    // =========================
    // TRAINER OVERLOAD
    // =========================

    @Test
    void shouldDetectTrainerOverload() {
        when(sessionRepository.findByTrainerIdAndCreatedAt(any(), any()))
                .thenReturn(List.of(new Session(), new Session(), new Session()));

        boolean result = service.checkTrainerOverload("trainer1", LocalDate.now(), null);

        assertTrue(result);
    }

    // =========================
    // SCHEDULING CONFLICT
    // =========================

    @Test
    void shouldDetectSchedulingConflict() {
        when(sessionRepository.findByTrainerIdAndCreatedAt(any(), any()))
                .thenReturn(List.of(new Session()));

        when(sessionRepository.findByPlanning_Location_IdAndCreatedAt(any(), any()))
                .thenReturn(List.of());

        boolean conflict = service.hasSchedulingConflict("trainer1", 1L, LocalDate.now(), null);

        assertTrue(conflict);
    }

    // =========================
    // STATUS UPDATE
    // =========================

    @Test
    void shouldUpdateSessionStatuses() {
        Session s = new Session();
        s.setCreatedAt(LocalDate.now());
        s.setStatus(SessionStatus.PLANNED);

        when(sessionRepository.findAll()).thenReturn(List.of(s));

        service.updateSessionStatuses();

        verify(sessionRepository).saveAll(any());
    }

    // =========================
    // CANCEL SESSION
    // =========================

    @Test
    void shouldCancelSession() {
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(sessionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.cancelSession(1L);

        assertEquals(SessionStatus.CANCELED, session.getStatus());
    }

    // =========================
    // PLANNING BASED STATUS
    // =========================

    @Test
    void shouldReturnSessionsStartingToday() {
        Planning planning = new Planning();
        planning.setStartDate(LocalDate.now());
        planning.setSession(session);

        when(planningRepository.findByStartDate(any()))
                .thenReturn(List.of(planning));

        List<Session> result = service.getSessionsStartingToday();

        assertEquals(1, result.size());
    }
}
