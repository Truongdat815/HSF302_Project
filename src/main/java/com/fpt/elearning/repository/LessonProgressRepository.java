package com.fpt.elearning.repository;

import com.fpt.elearning.entity.LessonProgress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LessonProgressRepository extends JpaRepository<LessonProgress, Long> {

    Optional<LessonProgress> findByEnrollment_IdAndLesson_Id(Long enrollmentId, Long lessonId);

    long countByEnrollment_IdAndCompletedTrue(Long enrollmentId);

    List<LessonProgress> findByEnrollment_Id(Long enrollmentId);
}
