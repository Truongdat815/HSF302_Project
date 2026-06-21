package com.fpt.elearning.repository;

import com.fpt.elearning.entity.QuizAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Optional;

public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Long> {

    void deleteByLesson_IdIn(Collection<Long> lessonIds);

    boolean existsByStudent_IdAndLesson_IdAndPassedTrue(Long studentId, Long lessonId);

    Optional<QuizAttempt> findTopByStudent_IdAndLesson_IdOrderByAttemptedAtDesc(Long studentId, Long lessonId);
}
