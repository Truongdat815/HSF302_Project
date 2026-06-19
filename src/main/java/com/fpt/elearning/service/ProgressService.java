package com.fpt.elearning.service;

import com.fpt.elearning.entity.Enrollment;
import com.fpt.elearning.entity.Lesson;
import com.fpt.elearning.entity.LessonProgress;
import com.fpt.elearning.entity.User;
import com.fpt.elearning.repository.EnrollmentRepository;
import com.fpt.elearning.repository.LessonProgressRepository;
import com.fpt.elearning.repository.LessonRepository;
import com.fpt.elearning.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ProgressService {

    private final EnrollmentRepository enrollmentRepository;
    private final LessonRepository lessonRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final QuestionRepository questionRepository;
    private final CertificateService certificateService;

    /**
     * Hoan thanh bai hoc bang nut "Danh dau hoan thanh".
     * Neu bai hoc CO bai kiem tra -> bat buoc lam quiz, khong cho danh dau tay.
     */
    @Transactional
    public void completeLesson(User user, Long courseId, Long lessonId) {
        Enrollment enrollment = enrollmentRepository
                .findByStudent_IdAndCourse_Id(user.getId(), courseId)
                .orElseThrow(() -> new IllegalStateException("Ban chua so huu khoa hoc nay"));
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay bai hoc"));

        if (questionRepository.countByLesson_Id(lessonId) > 0) {
            throw new IllegalStateException("Bai hoc nay co bai kiem tra, hay lam bai de hoan thanh.");
        }
        markComplete(enrollment, lesson);
    }

    /**
     * Danh dau bai hoc hoan thanh + tinh lai % + cap chung chi neu du 100%.
     * Dung chung cho ca nut thu cong va khi lam quiz dat diem.
     */
    @Transactional
    public void markComplete(Enrollment enrollment, Lesson lesson) {
        Long courseId = enrollment.getCourse().getId();

        LessonProgress lp = lessonProgressRepository
                .findByEnrollment_IdAndLesson_Id(enrollment.getId(), lesson.getId())
                .orElseGet(() -> LessonProgress.builder().enrollment(enrollment).lesson(lesson).build());
        lp.setCompleted(true);
        lp.setCompletedAt(LocalDateTime.now());
        lessonProgressRepository.save(lp);

        long total = lessonRepository.countByCourse_Id(courseId);
        long done = lessonProgressRepository.countByEnrollment_IdAndCompletedTrue(enrollment.getId());
        int percent = total == 0 ? 0 : (int) Math.round(done * 100.0 / total);
        enrollment.setProgress(percent);
        enrollmentRepository.save(enrollment);

        if (percent >= 100) {
            certificateService.issueIfAbsent(enrollment);
        }
    }
}
