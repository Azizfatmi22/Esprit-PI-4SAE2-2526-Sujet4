package com.formini.msliveclass.entities;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"poll_id", "voter_id"}))
public class PollVote {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "poll_id")
    private Long pollId;
    
    @Column(name = "voter_id")
    private String voterId;
    
    private Integer optionIndex;
    private LocalDateTime votedAt;

    public PollVote() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getPollId() { return pollId; }
    public void setPollId(Long pollId) { this.pollId = pollId; }

    public String getVoterId() { return voterId; }
    public void setVoterId(String voterId) { this.voterId = voterId; }

    public Integer getOptionIndex() { return optionIndex; }
    public void setOptionIndex(Integer optionIndex) { this.optionIndex = optionIndex; }

    public LocalDateTime getVotedAt() { return votedAt; }
    public void setVotedAt(LocalDateTime votedAt) { this.votedAt = votedAt; }
}
