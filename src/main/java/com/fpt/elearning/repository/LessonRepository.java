package com.fpt.elearning.repository;

import com.fpt.elearning.entity.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LessonRepository extends JpaRepository<Lesson, Long> {
    List<Lesson> findByCourse_IdOrderByOrderIndexAsc(Long courseId);
    long countByCourse_Id(Long courseId);
}
