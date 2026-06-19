package com.fpt.elearning.controller;

import com.fpt.elearning.dto.CourseForm;
import com.fpt.elearning.entity.*;
import com.fpt.elearning.entity.enums.CourseStatus;
import com.fpt.elearning.entity.enums.DiscountType;
import com.fpt.elearning.repository.*;
import com.fpt.elearning.service.AuthHelper;
import com.fpt.elearning.service.CourseService;
import com.fpt.elearning.util.HtmlSanitizer;
import com.fpt.elearning.util.SlugUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final CourseService courseService;
    private final CourseRepository courseRepository;
    private final CategoryRepository categoryRepository;
    private final LessonRepository lessonRepository;
    private final CouponRepository couponRepository;
    private final UserRepository userRepository;
    private final AuthHelper authHelper;

    // ===== Dashboard =====
    @GetMapping
    public String dashboard(Model model) {
        model.addAttribute("courseCount", courseRepository.count());
        model.addAttribute("categoryCount", categoryRepository.count());
        model.addAttribute("userCount", userRepository.count());
        model.addAttribute("couponCount", couponRepository.count());
        return "admin/dashboard";
    }

    // ===== COURSE CRUD =====
    @GetMapping("/courses")
    public String courses(Model model) {
        model.addAttribute("courses", courseRepository.findAll());
        return "admin/courses";
    }

    @GetMapping("/courses/new")
    public String newCourse(Model model) {
        model.addAttribute("form", new CourseForm());
        model.addAttribute("categories", categoryRepository.findAll());
        model.addAttribute("statuses", CourseStatus.values());
        return "admin/course-form";
    }

    @PostMapping("/courses")
    public String createCourse(@ModelAttribute("form") CourseForm form,
                               RedirectAttributes ra) throws IOException {
        courseService.create(form, authHelper.requireCurrentUser());
        ra.addFlashAttribute("success", "Tạo khóa học thành công!");
        return "redirect:/admin/courses";
    }

    @GetMapping("/courses/{id}/edit")
    public String editCourse(@PathVariable Long id, Model model, RedirectAttributes ra) {
        Course course = courseRepository.findById(id).orElse(null);
        if (course == null) {
            ra.addFlashAttribute("error", "Không tìm thấy khóa học");
            return "redirect:/admin/courses";
        }
        CourseForm form = new CourseForm();
        form.setId(course.getId());
        form.setTitle(course.getTitle());
        form.setShortDescription(course.getShortDescription());
        form.setDescription(course.getDescription());
        form.setPrice(course.getPrice());
        form.setStatus(course.getStatus());
        form.setCategoryId(course.getCategory() != null ? course.getCategory().getId() : null);
        model.addAttribute("form", form);
        model.addAttribute("course", course);
        model.addAttribute("categories", categoryRepository.findAll());
        model.addAttribute("statuses", CourseStatus.values());
        return "admin/course-form";
    }

    @PostMapping("/courses/{id}")
    public String updateCourse(@PathVariable Long id,
                               @ModelAttribute("form") CourseForm form,
                               RedirectAttributes ra) throws IOException {
        courseService.update(id, form);
        ra.addFlashAttribute("success", "Cập nhật khóa học thành công!");
        return "redirect:/admin/courses";
    }

    @PostMapping("/courses/{id}/delete")
    public String deleteCourse(@PathVariable Long id, RedirectAttributes ra) {
        courseService.delete(id);
        ra.addFlashAttribute("success", "Đã xóa khóa học");
        return "redirect:/admin/courses";
    }

    // ===== LESSONS =====
    @GetMapping("/courses/{courseId}/lessons")
    public String lessons(@PathVariable Long courseId, Model model, RedirectAttributes ra) {
        Course course = courseRepository.findById(courseId).orElse(null);
        if (course == null) {
            ra.addFlashAttribute("error", "Không tìm thấy khóa học");
            return "redirect:/admin/courses";
        }
        model.addAttribute("course", course);
        model.addAttribute("lessons", lessonRepository.findByCourse_IdOrderByOrderIndexAsc(courseId));
        return "admin/lessons";
    }

    @PostMapping("/courses/{courseId}/lessons")
    public String addLesson(@PathVariable Long courseId,
                            @RequestParam String title,
                            @RequestParam(required = false) String content,
                            @RequestParam(required = false) String videoUrl,
                            @RequestParam(defaultValue = "0") Integer orderIndex,
                            @RequestParam(defaultValue = "false") boolean preview,
                            RedirectAttributes ra) {
        Course course = courseRepository.findById(courseId).orElse(null);
        if (course == null) {
            ra.addFlashAttribute("error", "Không tìm thấy khóa học");
            return "redirect:/admin/courses";
        }
        Lesson lesson = Lesson.builder()
                .course(course)
                .title(title)
                .content(HtmlSanitizer.clean(content))  // rich text
                .videoUrl(videoUrl)
                .orderIndex(orderIndex)
                .preview(preview)
                .build();
        lessonRepository.save(lesson);
        ra.addFlashAttribute("success", "Đã thêm bài học");
        return "redirect:/admin/courses/" + courseId + "/lessons";
    }

    @PostMapping("/lessons/{lessonId}/delete")
    public String deleteLesson(@PathVariable Long lessonId,
                               @RequestParam Long courseId,
                               RedirectAttributes ra) {
        lessonRepository.deleteById(lessonId);
        ra.addFlashAttribute("success", "Đã xóa bài học");
        return "redirect:/admin/courses/" + courseId + "/lessons";
    }

    // ===== CATEGORIES =====
    @GetMapping("/categories")
    public String categories(Model model) {
        model.addAttribute("categories", categoryRepository.findAll());
        return "admin/categories";
    }

    @PostMapping("/categories")
    public String addCategory(@RequestParam String name, RedirectAttributes ra) {
        String slug = SlugUtil.toSlug(name);
        if (categoryRepository.existsBySlug(slug)) {
            ra.addFlashAttribute("error", "Danh mục đã tồn tại");
        } else {
            categoryRepository.save(Category.builder().name(name).slug(slug).build());
            ra.addFlashAttribute("success", "Đã thêm danh mục");
        }
        return "redirect:/admin/categories";
    }

    // ===== COUPONS =====
    @GetMapping("/coupons")
    public String coupons(Model model) {
        model.addAttribute("coupons", couponRepository.findAll());
        model.addAttribute("types", DiscountType.values());
        return "admin/coupons";
    }

    @PostMapping("/coupons")
    public String addCoupon(@RequestParam String code,
                            @RequestParam DiscountType discountType,
                            @RequestParam BigDecimal value,
                            @RequestParam(required = false) Integer usageLimit,
                            @RequestParam(required = false)
                            @org.springframework.format.annotation.DateTimeFormat(iso =
                                    org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
                            LocalDate expiryDate,
                            RedirectAttributes ra) {
        if (couponRepository.findByCodeIgnoreCase(code).isPresent()) {
            ra.addFlashAttribute("error", "Mã giảm giá đã tồn tại");
            return "redirect:/admin/coupons";
        }
        Coupon coupon = Coupon.builder()
                .code(code.toUpperCase())
                .discountType(discountType)
                .value(value)
                .usageLimit(usageLimit)
                .usedCount(0)
                .expiryDate(expiryDate != null ? LocalDateTime.of(expiryDate, LocalTime.MAX) : null)
                .active(true)
                .build();
        couponRepository.save(coupon);
        ra.addFlashAttribute("success", "Đã thêm mã giảm giá");
        return "redirect:/admin/coupons";
    }
}
