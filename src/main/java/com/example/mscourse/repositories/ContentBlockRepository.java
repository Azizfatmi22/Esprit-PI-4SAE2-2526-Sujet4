package com.example.mscourse.repositories;

import com.example.mscourse.entities.ContentBlock;
import com.example.mscourse.entities.ContentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContentBlockRepository extends JpaRepository<ContentBlock, Long> {

    List<ContentBlock> findByChapterIdOrderByOrderIndexAsc(Long chapterId);

    List<ContentBlock> findByChapterIdAndType(Long chapterId, ContentType type);

    @Query("SELECT MAX(c.orderIndex) FROM ContentBlock c WHERE c.chapter.id = :chapterId")
    Integer getMaxOrderIndex(@Param("chapterId") Long chapterId);

    @Modifying
    @Query("UPDATE ContentBlock c SET c.orderIndex = c.orderIndex + :delta " +
            "WHERE c.chapter.id = :chapterId AND c.orderIndex BETWEEN :start AND :end")
    void shiftOrderIndices(@Param("chapterId") Long chapterId,
                           @Param("start") Integer start,
                           @Param("end") Integer end,
                           @Param("delta") Integer delta);

    @Modifying
    @Query("DELETE FROM ContentBlock c WHERE c.chapter.id = :chapterId")
    void deleteByChapterId(@Param("chapterId") Long chapterId);

    @Query("SELECT c FROM ContentBlock c WHERE c.chapter.id = :chapterId AND " +
            "(LOWER(c.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(c.data) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<ContentBlock> searchContentByChapterId(@Param("chapterId") Long chapterId,
                                                @Param("keyword") String keyword);

    @Query("SELECT c FROM ContentBlock c WHERE c.chapter.course.id = :courseId AND " +
            "(LOWER(c.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(c.data) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<ContentBlock> searchContentByCourseId(@Param("courseId") Long courseId,
                                               @Param("keyword") String keyword);

    @Query("SELECT COUNT(c) FROM ContentBlock c WHERE c.chapter.id = :chapterId")
    Long countByChapterId(@Param("chapterId") Long chapterId);

    @Query("SELECT c.type, COUNT(c) FROM ContentBlock c WHERE c.chapter.id = :chapterId GROUP BY c.type")
    List<Object[]> getContentBlockStatistics(@Param("chapterId") Long chapterId);
}