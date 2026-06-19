package com.fpt.elearning.repository;

import com.fpt.elearning.entity.Certificate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CertificateRepository extends JpaRepository<Certificate, Long> {
    Optional<Certificate> findByEnrollment_Id(Long enrollmentId);
    Optional<Certificate> findByCode(String code);
}
