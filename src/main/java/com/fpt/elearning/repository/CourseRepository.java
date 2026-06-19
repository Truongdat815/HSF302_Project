package com.fpt.elearning.repository;

import com.fpt.elearning.entity.Course;
import com.fpt.elearning.entity.enums.CourseStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long> {

    Optional<Course> findBySlug(String slug);

    boolean existsBySlug(String slug);

    Page<Course> findByStatus(CourseStatus status, Pageable pageable);

    List<Course> findByStatus(CourseStatus status);

    Page<Course> findByStatusAndCategory_Slug(CourseStatus status, String categorySlug, Pageable pageable);

    // Tim kiem theo tu khoa (dung cho ca trang search va RAG retrieval)
    @Query("""
            SELECT c FROM Course c
            WHERE c.status = :status
              AND (LOWER(c.title) LIKE LOWER(CONCAT('%', :kw, '%'))
                   OR LOWER(c.shortDescription) LIKE LOWER(CONCAT('%', :kw, '%')))
            """)
    List<Course> searchPublished(@Param("status") CourseStatus status, @Param("kw") String keyword);
}
