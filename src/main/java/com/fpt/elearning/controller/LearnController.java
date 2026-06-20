package com.fpt.elearning.controller;

import com.fpt.elearning.entity.*;
import com.fpt.elearning.repository.*;
import com.fpt.elearning.service.AuthHelper;
import com.fpt.elearning.service.ProgressService;
import com.fpt.elearning.service.QuizService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class LearnController {

    private final AuthHelper authHelper;
    private final EnrollmentRepository enrollmentRepository;
    private final LessonRepository lessonRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final CertificateRepository certificateRepository;
    private final QuestionRepository questionRepository;
    private final ProgressService progressService;
    private final QuizService quizService;

    @GetMapping("/my-courses")
    @Transactional(readOnly = true)
    public String myCourses(Model model) {
        User user = authHelper.requireCurrentUser();
        model.addAttribute("enrollments", enrollmentRepository.findByStudent_Id(user.getId()));
        return "learn/my-courses";
    }

    @GetMapping("/learn/{courseId}")
    @Transactional(readOnly = true)
    public String learn(@PathVariable Long courseId,
                        @RequestParam(required = false) Long lesson,
                        Model model, RedirectAttributes ra) {
        User user = authHelper.requireCurrentUser();
        Enrollment enrollment = enrollmentRepository
                .findByStudent_IdAndCourse_Id(user.getId(), courseId).orElse(null);
        if (enrollment == null) {
            ra.addFlashAttribute("error", "Ban chua so huu khoa hoc nay.");
            return "redirect:/courses";
        }

        List<Lesson> lessons = lessonRepository.findByCourse_IdOrderByOrderIndexAsc(courseId);
        Set<Long> completedIds = lessonProgressRepository.findByEnrollment_Id(enrollment.getId())
                .stream().filter(LessonProgress::isCompleted)
                .map(lp -> lp.getLesson().getId()).collect(Collectors.toSet());

        // Tap cac bai hoc co bai kiem tra
        Set<Long> quizLessonIds = lessons.stream()
                .filter(l -> questionRepository.countByLesson_Id(l.getId()) > 0)
                .map(Lesson::getId).collect(Collectors.toSet());

        Lesson current = lessons.stream()
                .filter(l -> lesson != null && l.getId().equals(lesson))
                .findFirst()
                .orElse(lessons.isEmpty() ? null : lessons.get(0));

        model.addAttribute("enrollment", enrollment);
        model.addAttribute("course", enrollment.getCourse());
        model.addAttribute("lessons", lessons);
        model.addAttribute("completedIds", completedIds);
        model.addAttribute("quizLessonIds", quizLessonIds);
        model.addAttribute("current", current);
        model.addAttribute("currentHasQuiz", current != null && quizLessonIds.contains(current.getId()));

        // Nhung video: YouTube/Vimeo -> iframe; file mp4... -> the <video>
        String videoUrl = current != null ? current.getVideoUrl() : null;
        String videoEmbed = com.fpt.elearning.util.VideoUtil.toEmbedUrl(videoUrl);
        model.addAttribute("videoEmbed", videoEmbed);
        model.addAttribute("videoIsFile", videoEmbed == null && com.fpt.elearning.util.VideoUtil.isDirectVideo(videoUrl));
        return "learn/course";
    }

    @PostMapping("/learn/{courseId}/lessons/{lessonId}/complete")
    public String complete(@PathVariable Long courseId, @PathVariable Long lessonId,
                           RedirectAttributes ra) {
        User user = authHelper.requireCurrentUser();
        try {
            progressService.completeLesson(user, courseId, lessonId);
            ra.addFlashAttribute("success", "Da hoan thanh bai hoc!");
        } catch (RuntimeException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/learn/" + courseId + "?lesson=" + lessonId;
    }

    // ===== QUIZ =====
    @GetMapping("/learn/{courseId}/lessons/{lessonId}/quiz")
    @Transactional(readOnly = true)
    public String quiz(@PathVariable Long courseId, @PathVariable Long lessonId,
                       Model model, RedirectAttributes ra) {
        User user = authHelper.requireCurrentUser();
        if (!enrollmentRepository.existsByStudent_IdAndCourse_Id(user.getId(), courseId)) {
            ra.addFlashAttribute("error", "Ban chua so huu khoa hoc nay.");
            return "redirect:/courses";
        }
        Lesson lesson = lessonRepository.findById(lessonId).orElse(null);
        List<Question> questions = questionRepository.findByLesson_IdOrderByIdAsc(lessonId);
        questions.forEach(q -> q.getChoices().size());
        if (lesson == null || questions.isEmpty()) {
            ra.addFlashAttribute("error", "Bai hoc nay chua co bai kiem tra.");
            return "redirect:/learn/" + courseId + "?lesson=" + lessonId;
        }
        model.addAttribute("courseId", courseId);
        model.addAttribute("lesson", lesson);
        model.addAttribute("questions", questions);
        return "learn/quiz";
    }

    @PostMapping("/learn/{courseId}/lessons/{lessonId}/quiz")
    public String submitQuiz(@PathVariable Long courseId, @PathVariable Long lessonId,
                             @RequestParam Map<String, String> params,
                             RedirectAttributes ra) {
        User user = authHelper.requireCurrentUser();

        // Doc cac dap an dang q_{questionId}={choiceId}
        Map<Long, Long> answers = new HashMap<>();
        params.forEach((k, v) -> {
            if (k.startsWith("q_") && v != null && !v.isBlank()) {
                try {
                    answers.put(Long.parseLong(k.substring(2)), Long.parseLong(v));
                } catch (NumberFormatException ignored) {
                }
            }
        });

        try {
            QuizService.QuizResult r = quizService.grade(user, courseId, lessonId, answers);
            String msg = "Diem cua ban: " + r.score() + "% (" + r.correct() + "/" + r.total() + " cau dung).";
            if (r.passed()) {
                ra.addFlashAttribute("success", msg + " Dat yeu cau - da hoan thanh bai hoc!");
            } else {
                ra.addFlashAttribute("error", msg + " Chua dat (can >= " + r.passScore() + "%). Hay lam lai.");
            }
        } catch (RuntimeException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/learn/" + courseId + "?lesson=" + lessonId;
    }

    @GetMapping("/certificate/{enrollmentId}")
    @Transactional(readOnly = true)
    public String certificate(@PathVariable Long enrollmentId, Model model, RedirectAttributes ra) {
        User user = authHelper.requireCurrentUser();
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId).orElse(null);
        if (enrollment == null || !enrollment.getStudent().getId().equals(user.getId())) {
            ra.addFlashAttribute("error", "Khong tim thay chung chi.");
            return "redirect:/my-courses";
        }
        Certificate cert = certificateRepository.findByEnrollment_Id(enrollmentId).orElse(null);
        if (cert == null) {
            ra.addFlashAttribute("error", "Ban can hoan thanh 100% khoa hoc de nhan chung chi.");
            return "redirect:/my-courses";
        }
        model.addAttribute("certificate", cert);
        model.addAttribute("enrollment", enrollment);
        return "learn/certificate";
    }
}
