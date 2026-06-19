package com.fpt.elearning.repository;

import com.fpt.elearning.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long> {

    List<Question> findByLesson_IdOrderByIdAsc(Long lessonId);

    long countByLesson_Id(Long lessonId);
}
