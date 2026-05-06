package com.sessionmanagementservice.Repos;

import com.sessionmanagementservice.Repositories.SessionRepository;
import com.sessionmanagementservice.entities.Session;
import com.sessionmanagementservice.entities.SessionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class SessionRepositoryTest {

    @Autowired
    private SessionRepository sessionRepository;

    private Session session;

    @BeforeEach
    void setUp() {
        session = new Session();
        session.setTrainerId("trainer1");
        session.setCreatedAt(LocalDate.now());
        session.setStatus(SessionStatus.PLANNED);
        session.setMaxParticipants(20);
        session = sessionRepository.save(session);
    }

    @Test
    void shouldFindByTrainerIdAndDate() {
        var result = sessionRepository.findByTrainerIdAndCreatedAt("trainer1", LocalDate.now());

        assertFalse(result.isEmpty());
        assertEquals("trainer1", result.get(0).getTrainerId());
    }

    @Test
    void shouldFindByStatus() {
        var result = sessionRepository.findByStatus(SessionStatus.PLANNED);

        assertFalse(result.isEmpty());
        assertEquals(SessionStatus.PLANNED, result.get(0).getStatus());
    }

    @Test
    void shouldNotFindByTrainerIdAndDateWhenNotExists() {
        var result = sessionRepository.findByTrainerIdAndCreatedAt("nonexistent", LocalDate.now());

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldNotFindByStatusWhenNotExists() {
        var result = sessionRepository.findByStatus(SessionStatus.COMPLETED);

        assertTrue(result.isEmpty());
    }
}