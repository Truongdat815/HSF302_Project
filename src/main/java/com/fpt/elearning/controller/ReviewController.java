package com.fpt.elearning.controller;

import com.fpt.elearning.entity.Course;
import com.fpt.elearning.entity.User;
import com.fpt.elearning.repository.CourseRepository;
import com.fpt.elearning.service.AuthHelper;
import com.fpt.elearning.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;
    private final CourseRepository courseRepository;
    private final AuthHelper authHelper;

    @PostMapping("/reviews")
    public String addReview(@RequestParam Long courseId,
                            @RequestParam int rating,
                            @RequestParam(required = false) String comment,
                            RedirectAttributes ra) {
        User user = authHelper.requireCurrentUser();
        String slug = courseRepository.findById(courseId).map(Course::getSlug).orElse(null);
        try {
            reviewService.addOrUpdate(user, courseId, rating, comment);
            ra.addFlashAttribute("success", "Cảm ơn bạn đã đánh giá!");
        } catch (RuntimeException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return slug != null ? "redirect:/courses/" + slug : "redirect:/courses";
    }
}
