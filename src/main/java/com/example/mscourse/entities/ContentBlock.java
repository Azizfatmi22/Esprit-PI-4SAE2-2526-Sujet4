package com.example.mscourse.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "content_blocks", indexes = {
        @Index(name = "idx_content_chapter", columnList = "chapter_id"),
        @Index(name = "idx_content_type", columnList = "type")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContentBlock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ContentType type;

    @Column(name = "order_index")
    private Integer orderIndex;

    @Column(columnDefinition = "LONGTEXT")
    private String data;

    @Column(length = 200)
    private String title;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chapter_id", nullable = false)
    private Chapter chapter;


}