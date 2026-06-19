package com.fpt.elearning.service;

import com.fpt.elearning.entity.Certificate;
import com.fpt.elearning.entity.Enrollment;
import com.fpt.elearning.repository.CertificateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CertificateService {

    private final CertificateRepository certificateRepository;

    /**
     * Cap chung chi cho enrollment neu chua co (goi khi progress = 100%).
     */
    @Transactional
    public Certificate issueIfAbsent(Enrollment enrollment) {
        return certificateRepository.findByEnrollment_Id(enrollment.getId())
                .orElseGet(() -> certificateRepository.save(Certificate.builder()
                        .enrollment(enrollment)
                        .code("CERT-" + UUID.randomUUID().toString()
                                .replace("-", "").substring(0, 12).toUpperCase())
                        .build()));
    }
}
