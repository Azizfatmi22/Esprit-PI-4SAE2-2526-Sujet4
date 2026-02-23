package com.example.mscourse.repositories;

import com.example.mscourse.entities.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ContentBlockRepository extends JpaRepository<ContentBlock, Long> {

    // Find all content blocks of a chapter
    List<ContentBlock> findByChapterId(Long chapterId);

    // Find content blocks by type in a chapter
    List<ContentBlock> findByChapterIdAndType(Long chapterId, ContentType type);

    // Delete all content blocks of a chapter
    void deleteByChapterId(Long chapterId);
}