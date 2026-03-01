package com.example.mscourse.repositories;

import com.example.mscourse.entities.AttachmentCategory;
import com.example.mscourse.entities.CourseAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface CourseAttachmentRepository extends JpaRepository<CourseAttachment, Long> {

    // Find attachments by course
    List<CourseAttachment> findByCourseId(Long courseId);

    // Find attachments by course and category
    List<CourseAttachment> findByCourseIdAndCategory(Long courseId, AttachmentCategory category);

    // Find attachments by category
    List<CourseAttachment> findByCategory(AttachmentCategory category);

    // Find attachments by file type
    List<CourseAttachment> findByFileType(String fileType);

    // Delete all attachments of a course
    @Modifying
    @Transactional
    @Query("DELETE FROM CourseAttachment a WHERE a.course.id = :courseId")
    void deleteByCourseId(@Param("courseId") Long courseId);

    // Count attachments by course
    Long countByCourseId(Long courseId);

    // Count attachments by course and category
    Long countByCourseIdAndCategory(Long courseId, AttachmentCategory category);

    // Get total size of attachments in a course
    @Query("SELECT COALESCE(SUM(a.fileSize), 0) FROM CourseAttachment a WHERE a.course.id = :courseId")
    Long getTotalSizeByCourseId(@Param("courseId") Long courseId);

    // Group attachments by category in a course
    @Query("SELECT a.category, COUNT(a), COALESCE(SUM(a.fileSize), 0) FROM CourseAttachment a WHERE a.course.id = :courseId GROUP BY a.category")
    List<Object[]> getAttachmentStatisticsByCourse(@Param("courseId") Long courseId);

    // Find attachments by course with size greater than
    @Query("SELECT a FROM CourseAttachment a WHERE a.course.id = :courseId AND a.fileSize > :minSize")
    List<CourseAttachment> findByCourseIdAndMinSize(@Param("courseId") Long courseId, @Param("minSize") Long minSize);

    // Find attachments by course and filename containing
    List<CourseAttachment> findByCourseIdAndFileNameContaining(Long courseId, String fileName);
}