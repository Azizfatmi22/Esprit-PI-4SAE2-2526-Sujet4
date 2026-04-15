package com.example.msforum.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "comments")
@Getter
@Setter
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    private String userId;

    private String content;

    private Boolean isBestAnswer = false;

    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    private ContentStatus status = ContentStatus.APPROVED;

    private Boolean reviewedByAdmin = false;

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
