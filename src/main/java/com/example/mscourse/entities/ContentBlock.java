package com.example.mscourse.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "content_blocks")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContentBlock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private ContentType type; // text, image, video, resource, pdf

    private Integer orderIndex; // To order content within chapter

    @Column(columnDefinition = "TEXT") // For long text
    private String data; // The actual content (text, URL, etc.)

    private String title; // Optional title for the block

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chapter_id", nullable = false)
    private Chapter chapter;
}
