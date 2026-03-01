// ChapterEntity.java
package com.example.mscourse.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "chapters", indexes = {
        @Index(name = "idx_chapter_course", columnList = "course_id"),
        @Index(name = "idx_chapter_order", columnList = "order_index")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Chapter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "order_index")
    private Integer orderIndex;

    @Column(name = "description", length = 500)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @OneToMany(mappedBy = "chapter", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    private List<ContentBlock> contentBlocks = new ArrayList<>();

    // Helper methods
    public void addContentBlock(ContentBlock contentBlock) {
        contentBlocks.add(contentBlock);
        contentBlock.setChapter(this);
    }

    public void removeContentBlock(ContentBlock contentBlock) {
        contentBlocks.remove(contentBlock);
        contentBlock.setChapter(null);
    }
}