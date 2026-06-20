package com.fpt.elearning.controller;

import com.fpt.elearning.dto.CourseForm;
import com.fpt.elearning.entity.*;
import com.fpt.elearning.entity.enums.CourseStatus;
import com.fpt.elearning.entity.enums.DiscountType;
import com.fpt.elearning.entity.enums.OrderStatus;
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
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    private final OrderRepository orderRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CertificateRepository certificateRepository;
    private final AuthHelper authHelper;

    // ===== Dashboard =====
    @GetMapping
    public String dashboard(Model model) {
        LocalDate today = LocalDate.now();
        LocalDateTime weekStart = today.minusDays(6).atStartOfDay();
        LocalDateTime monthStart = today.withDayOfMonth(1).atStartOfDay();

        List<Order> paidOrders = orderRepository.findAll().stream()
                .filter(o -> o.getStatus() == OrderStatus.PAID)
                .sorted(Comparator.comparing(Order::getCreatedAt).reversed())
                .toList();

        BigDecimal totalRevenue = sumOrders(paidOrders);
        BigDecimal weekRevenue = sumOrders(paidOrders.stream()
                .filter(o -> !o.getCreatedAt().isBefore(weekStart)).toList());
        BigDecimal monthRevenue = sumOrders(paidOrders.stream()
                .filter(o -> !o.getCreatedAt().isBefore(monthStart)).toList());

        List<RevenuePoint> weeklyRevenue = today.minusDays(6).datesUntil(today.plusDays(1))
                .map(day -> new RevenuePoint(
                        day.getDayOfMonth() + "/" + day.getMonthValue(),
                        sumOrders(paidOrders.stream()
                                .filter(o -> o.getCreatedAt().toLocalDate().equals(day)).toList())))
                .toList();

        List<RevenuePoint> monthlyRevenue = java.util.stream.IntStream.rangeClosed(0, 5)
                .mapToObj(i -> YearMonth.from(today).minusMonths(5 - i))
                .map(month -> new RevenuePoint(
                        month.getMonthValue() + "/" + month.getYear(),
                        sumOrders(paidOrders.stream()
                                .filter(o -> YearMonth.from(o.getCreatedAt()).equals(month)).toList())))
                .toList();

        BigDecimal maxWeeklyRevenue = maxRevenue(weeklyRevenue);
        BigDecimal maxMonthlyRevenue = maxRevenue(monthlyRevenue);

        List<Enrollment> enrollments = enrollmentRepository.findAll().stream()
                .sorted(Comparator.comparing(Enrollment::getEnrolledAt).reversed())
                .toList();
        List<Enrollment> activeLearners = enrollments.stream()
                .filter(e -> e.getProgress() != null && e.getProgress() > 0 && e.getProgress() < 100)
                .limit(8)
                .toList();
        List<Enrollment> notStartedLearners = enrollments.stream()
                .filter(e -> e.getProgress() == null || e.getProgress() == 0)
                .limit(8)
                .toList();
        List<Certificate> certificates = certificateRepository.findAll().stream()
                .sorted(Comparator.comparing(Certificate::getIssuedAt).reversed())
                .limit(8)
                .toList();

        model.addAttribute("courseCount", courseRepository.count());
        model.addAttribute("categoryCount", categoryRepository.count());
        model.addAttribute("userCount", userRepository.count());
        model.addAttribute("couponCount", couponRepository.count());
        model.addAttribute("weekRevenue", weekRevenue);
        model.addAttribute("monthRevenue", monthRevenue);
        model.addAttribute("totalRevenue", totalRevenue);
        model.addAttribute("paidOrderCount", paidOrders.size());
        model.addAttribute("recentPaidOrders", paidOrders.stream().limit(6).toList());
        model.addAttribute("weeklyRevenue", withPercent(weeklyRevenue, maxWeeklyRevenue));
        model.addAttribute("monthlyRevenue", withPercent(monthlyRevenue, maxMonthlyRevenue));
        model.addAttribute("activeLearners", activeLearners);
        model.addAttribute("notStartedLearners", notStartedLearners);
        model.addAttribute("certificates", certificates);
        return "admin/dashboard";
    }

    private BigDecimal sumOrders(List<Order> orders) {
        return orders.stream()
                .map(Order::getTotalAmount)
                .filter(v -> v != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal maxRevenue(List<RevenuePoint> points) {
        return points.stream()
                .map(RevenuePoint::amount)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
    }

    private List<RevenuePoint> withPercent(List<RevenuePoint> points, BigDecimal max) {
        return points.stream()
                .map(p -> new RevenuePoint(p.label(), p.amount(), revenuePercent(p.amount(), max)))
                .toList();
    }

    private int revenuePercent(BigDecimal amount, BigDecimal max) {
        if (max == null || max.compareTo(BigDecimal.ZERO) <= 0 || amount == null) {
            return 0;
        }
        return amount.multiply(BigDecimal.valueOf(100))
                .divide(max, 0, RoundingMode.HALF_UP)
                .max(BigDecimal.valueOf(4))
                .intValue();
    }

    public record RevenuePoint(String label, BigDecimal amount, int percent) {
        public RevenuePoint(String label, BigDecimal amount) {
            this(label, amount, 0);
        }
    }

    @GetMapping("/users")
    public String users(Model model) {
        model.addAttribute("users", userRepository.findAll());
        return "admin/users";
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
        model.addAttribute("editLesson", null);
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

    @GetMapping("/lessons/{lessonId}/edit")
    public String editLesson(@PathVariable Long lessonId, Model model, RedirectAttributes ra) {
        Lesson lesson = lessonRepository.findById(lessonId).orElse(null);
        if (lesson == null) {
            ra.addFlashAttribute("error", "Không tìm thấy bài học");
            return "redirect:/admin/courses";
        }
        Course course = lesson.getCourse();
        model.addAttribute("course", course);
        model.addAttribute("lessons", lessonRepository.findByCourse_IdOrderByOrderIndexAsc(course.getId()));
        model.addAttribute("editLesson", lesson);
        return "admin/lessons";
    }

    @PostMapping("/lessons/{lessonId}")
    public String updateLesson(@PathVariable Long lessonId,
                               @RequestParam String title,
                               @RequestParam(required = false) String content,
                               @RequestParam(required = false) String videoUrl,
                               @RequestParam(defaultValue = "0") Integer orderIndex,
                               @RequestParam(defaultValue = "false") boolean preview,
                               RedirectAttributes ra) {
        Lesson lesson = lessonRepository.findById(lessonId).orElse(null);
        if (lesson == null) {
            ra.addFlashAttribute("error", "Không tìm thấy bài học");
            return "redirect:/admin/courses";
        }
        lesson.setTitle(title);
        lesson.setContent(HtmlSanitizer.clean(content));
        lesson.setVideoUrl(videoUrl);
        lesson.setOrderIndex(orderIndex);
        lesson.setPreview(preview);
        lessonRepository.save(lesson);
        ra.addFlashAttribute("success", "Đã cập nhật bài học");
        return "redirect:/admin/courses/" + lesson.getCourse().getId() + "/lessons";
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
        List<Category> categories = categoryRepository.findAll();
        Map<Long, Long> categoryCourseCounts = categories.stream()
                .collect(Collectors.toMap(Category::getId, c -> courseRepository.countByCategory_Id(c.getId())));
        model.addAttribute("categories", categories);
        model.addAttribute("categoryCourseCounts", categoryCourseCounts);
        model.addAttribute("editCategory", null);
        return "admin/categories";
    }

    @GetMapping("/categories/{id}/edit")
    public String editCategory(@PathVariable Long id, Model model, RedirectAttributes ra) {
        Category editCategory = categoryRepository.findById(id).orElse(null);
        if (editCategory == null) {
            ra.addFlashAttribute("error", "Không tìm thấy danh mục");
            return "redirect:/admin/categories";
        }
        List<Category> categories = categoryRepository.findAll();
        Map<Long, Long> categoryCourseCounts = categories.stream()
                .collect(Collectors.toMap(Category::getId, c -> courseRepository.countByCategory_Id(c.getId())));
        model.addAttribute("categories", categories);
        model.addAttribute("categoryCourseCounts", categoryCourseCounts);
        model.addAttribute("editCategory", editCategory);
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

    @PostMapping("/categories/{id}")
    public String updateCategory(@PathVariable Long id,
                                 @RequestParam String name,
                                 RedirectAttributes ra) {
        Category category = categoryRepository.findById(id).orElse(null);
        if (category == null) {
            ra.addFlashAttribute("error", "Không tìm thấy danh mục");
            return "redirect:/admin/categories";
        }
        String slug = SlugUtil.toSlug(name);
        Category existing = categoryRepository.findBySlug(slug).orElse(null);
        if (existing != null && !existing.getId().equals(id)) {
            ra.addFlashAttribute("error", "Danh mục đã tồn tại");
            return "redirect:/admin/categories/" + id + "/edit";
        }
        category.setName(name);
        category.setSlug(slug);
        categoryRepository.save(category);
        ra.addFlashAttribute("success", "Đã cập nhật danh mục");
        return "redirect:/admin/categories";
    }

    @PostMapping("/categories/{id}/delete")
    public String deleteCategory(@PathVariable Long id, RedirectAttributes ra) {
        Category category = categoryRepository.findById(id).orElse(null);
        if (category == null) {
            ra.addFlashAttribute("error", "Không tìm thấy danh mục");
            return "redirect:/admin/categories";
        }
        long courseCount = courseRepository.countByCategory_Id(id);
        if (courseCount > 0) {
            ra.addFlashAttribute("error", "Không thể xóa danh mục đang có " + courseCount + " khóa học. Hãy đổi tên hoặc chuyển khóa học sang danh mục khác trước.");
            return "redirect:/admin/categories";
        }
        categoryRepository.delete(category);
        ra.addFlashAttribute("success", "Đã xóa danh mục");
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
