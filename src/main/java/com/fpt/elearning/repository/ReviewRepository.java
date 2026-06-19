package com.fpt.elearning.repository;

import com.fpt.elearning.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByCourse_IdOrderByCreatedAtDesc(Long courseId);

    boolean existsByCourse_IdAndStudent_Id(Long courseId, Long studentId);

    @Query("SELECT COALESCE(AVG(r.rating), 0) FROM Review r WHERE r.course.id = :courseId")
    Double averageRating(@Param("courseId") Long courseId);
}
