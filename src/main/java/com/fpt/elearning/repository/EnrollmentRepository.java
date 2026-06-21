package com.fpt.elearning.repository;

import com.fpt.elearning.entity.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    Optional<Enrollment> findByStudent_IdAndCourse_Id(Long studentId, Long courseId);

    boolean existsByStudent_IdAndCourse_Id(Long studentId, Long courseId);

    List<Enrollment> findByStudent_Id(Long studentId);

    List<Enrollment> findByCourse_Id(Long courseId);

    void deleteByCourse_Id(Long courseId);
}
