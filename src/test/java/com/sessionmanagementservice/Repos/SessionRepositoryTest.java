package com.sessionmanagementservice.Repos;



import com.sessionmanagementservice.Repositories.SessionRepository;
import com.sessionmanagementservice.entities.Session;
import com.sessionmanagementservice.entities.SessionStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class SessionRepositoryTest {

    @Autowired
    private SessionRepository sessionRepository;

    @Test
    void shouldFindByTrainerIdAndDate() {
        Session s = new Session();
        s.setTrainerId("trainer1");
        s.setCreatedAt(LocalDate.now());
        s.setStatus(SessionStatus.PLANNED);

        sessionRepository.save(s);

        assertFalse(
                sessionRepository.findByTrainerIdAndCreatedAt("trainer1", LocalDate.now()).isEmpty()
        );
    }

    @Test
    void shouldFindByStatus() {
        Session s = new Session();
        s.setTrainerId("trainer2");
        s.setCreatedAt(LocalDate.now());
        s.setStatus(SessionStatus.PLANNED);

        sessionRepository.save(s);

        assertFalse(sessionRepository.findByStatus(SessionStatus.PLANNED).isEmpty());
    }
}