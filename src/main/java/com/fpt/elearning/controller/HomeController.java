package com.fpt.elearning.controller;

import com.fpt.elearning.entity.Course;
import com.fpt.elearning.entity.User;
import com.fpt.elearning.entity.enums.CourseStatus;
import com.fpt.elearning.repository.CourseRepository;
import com.fpt.elearning.repository.EnrollmentRepository;
import com.fpt.elearning.repository.LessonRepository;
import com.fpt.elearning.repository.ReviewRepository;
import com.fpt.elearning.service.AuthHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final CourseRepository courseRepository;
    private final ReviewRepository reviewRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final LessonRepository lessonRepository;
    private final AuthHelper authHelper;

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("courses", courseRepository.findByStatus(CourseStatus.PUBLISHED));
        return "home";
    }

    @GetMapping("/courses")
    public String courses(@RequestParam(required = false) String q, Model model) {
        List<Course> courses = (q != null && !q.isBlank())
                ? courseRepository.searchPublished(CourseStatus.PUBLISHED, q)
                : courseRepository.findByStatus(CourseStatus.PUBLISHED);
        model.addAttribute("courses", courses);
        model.addAttribute("q", q);
        return "course/list";
    }

    @GetMapping("/courses/{slug}")
    public String courseDetail(@PathVariable String slug, Model model, RedirectAttributes ra) {
        Course course = courseRepository.findBySlug(slug).orElse(null);
        if (course == null) {
            ra.addFlashAttribute("error", "Không tìm thấy khóa học");
            return "redirect:/courses";
        }

        boolean enrolled = false;
        User current = authHelper.currentUser();
        if (current != null) {
            enrolled = enrollmentRepository
                    .existsByStudent_IdAndCourse_Id(current.getId(), course.getId());
        }

        model.addAttribute("course", course);
        model.addAttribute("lessons", lessonRepository.findByCourse_IdOrderByOrderIndexAsc(course.getId()));
        model.addAttribute("reviews", reviewRepository.findByCourse_IdOrderByCreatedAtDesc(course.getId()));
        model.addAttribute("avgRating", reviewRepository.averageRating(course.getId()));
        model.addAttribute("enrolled", enrolled);
        return "course/detail";
    }
}
