package com.formini.msliveclass.repositories;

import com.formini.msliveclass.entities.Poll;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PollRepository extends JpaRepository<Poll, Long> {
    List<Poll> findBySessionId(Long sessionId);
    List<Poll> findBySessionIdAndIsActive(Long sessionId, Boolean isActive);
}
