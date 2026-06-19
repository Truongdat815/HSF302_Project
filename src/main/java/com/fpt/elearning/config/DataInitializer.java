package com.fpt.elearning.config;

import com.fpt.elearning.entity.*;
import com.fpt.elearning.entity.enums.CourseStatus;
import com.fpt.elearning.entity.enums.DiscountType;
import com.fpt.elearning.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * Seed du lieu mau khi khoi dong (chi chay khi DB trong).
 * Tài khoản admin:   admin@fpt.edu.vn / 123456
 * Tài khoản student: student@fpt.edu.vn / 123456
 */
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final CourseRepository courseRepository;
    private final CouponRepository couponRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return; // đã có dữ liệu -> bỏ qua
        }

        Role roleAdmin = roleRepository.save(Role.builder().name("ROLE_ADMIN").build());
        Role roleStudent = roleRepository.save(Role.builder().name("ROLE_STUDENT").build());

        User admin = userRepository.save(User.builder()
                .fullName("Quản trị viên")
                .email("admin@fpt.edu.vn")
                .password(passwordEncoder.encode("123456"))
                .roles(Set.of(roleAdmin))
                .enabled(true)
                .build());

        userRepository.save(User.builder()
                .fullName("Học viên Demo")
                .email("student@fpt.edu.vn")
                .password(passwordEncoder.encode("123456"))
                .roles(Set.of(roleStudent))
                .enabled(true)
                .build());

        Category programming = categoryRepository.save(
                Category.builder().name("Lập trình").slug("lap-trinh").build());
        Category design = categoryRepository.save(
                Category.builder().name("Thiết kế").slug("thiet-ke").build());

        courseRepository.save(Course.builder()
                .title("Java Spring Boot từ cơ bản đến nâng cao")
                .slug("java-spring-boot-co-ban-nang-cao")
                .shortDescription("Học xây dựng web app với Spring Boot, JPA, Security.")
                .description("<h2>Giới thiệu</h2><p>Khóa học giúp bạn "
                        + "<strong>thành thạo Spring Boot</strong> qua dự án thực tế. "
                        + "<span style=\"color:#e74c3c\">Phù hợp cho sinh viên FPT</span>.</p>"
                        + "<ul><li>Spring MVC + Thymeleaf</li><li>Spring Data JPA</li>"
                        + "<li>Spring Security</li></ul>")
                .price(new BigDecimal("499000"))
                .status(CourseStatus.PUBLISHED)
                .category(programming)
                .instructor(admin)
                .build());

        courseRepository.save(Course.builder()
                .title("Thiết kế UI/UX với Figma")
                .slug("thiet-ke-ui-ux-figma")
                .shortDescription("Làm chủ Figma và nguyên tắc thiết kế giao diện.")
                .description("<h2>Nội dung</h2><p>Từ <em>wireframe</em> đến "
                        + "<strong>prototype</strong> hoàn chỉnh.</p>")
                .price(new BigDecimal("350000"))
                .status(CourseStatus.PUBLISHED)
                .category(design)
                .instructor(admin)
                .build());

        couponRepository.save(Coupon.builder()
                .code("WELCOME10")
                .discountType(DiscountType.PERCENT)
                .value(new BigDecimal("10"))
                .expiryDate(LocalDateTime.now().plusMonths(3))
                .usageLimit(100)
                .usedCount(0)
                .active(true)
                .build());
    }
}
