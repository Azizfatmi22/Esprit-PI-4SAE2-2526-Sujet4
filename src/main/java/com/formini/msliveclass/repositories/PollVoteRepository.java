package com.formini.msliveclass.repositories;

import com.formini.msliveclass.entities.PollVote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PollVoteRepository extends JpaRepository<PollVote, Long> {
    List<PollVote> findByPollId(Long pollId);
    Optional<PollVote> findByPollIdAndVoterId(Long pollId, String voterId);
    long countByPollIdAndOptionIndex(Long pollId, Integer optionIndex);
}
