package com.fpt.elearning.service;

import com.fpt.elearning.dto.CourseForm;
import com.fpt.elearning.entity.Category;
import com.fpt.elearning.entity.Course;
import com.fpt.elearning.entity.Enrollment;
import com.fpt.elearning.entity.Lesson;
import com.fpt.elearning.entity.Question;
import com.fpt.elearning.entity.User;
import com.fpt.elearning.repository.CartItemRepository;
import com.fpt.elearning.repository.CategoryRepository;
import com.fpt.elearning.repository.CertificateRepository;
import com.fpt.elearning.repository.CourseRepository;
import com.fpt.elearning.repository.EnrollmentRepository;
import com.fpt.elearning.repository.LessonProgressRepository;
import com.fpt.elearning.repository.LessonRepository;
import com.fpt.elearning.repository.OrderItemRepository;
import com.fpt.elearning.repository.QuestionRepository;
import com.fpt.elearning.repository.QuizAttemptRepository;
import com.fpt.elearning.repository.ReviewRepository;
import com.fpt.elearning.util.HtmlSanitizer;
import com.fpt.elearning.util.SlugUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository courseRepository;
    private final CategoryRepository categoryRepository;
    private final CloudinaryService cloudinaryService;
    private final LessonRepository lessonRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CartItemRepository cartItemRepository;
    private final OrderItemRepository orderItemRepository;
    private final ReviewRepository reviewRepository;
    private final CertificateRepository certificateRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final QuestionRepository questionRepository;

    @Transactional
    public Course create(CourseForm form, User instructor) throws IOException {
        Course course = new Course();
        applyForm(course, form);
        course.setSlug(uniqueSlug(SlugUtil.toSlug(form.getTitle()), null));
        course.setInstructor(instructor);
        uploadThumbnailIfPresent(course, form.getThumbnail());
        return courseRepository.save(course);
    }

    @Transactional
    public Course update(Long id, CourseForm form) throws IOException {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khóa học"));
        applyForm(course, form);
        // Cập nhật slug nếu đổi tiêu đề
        String newSlug = SlugUtil.toSlug(form.getTitle());
        if (!newSlug.equals(course.getSlug())) {
            course.setSlug(uniqueSlug(newSlug, id));
        }
        uploadThumbnailIfPresent(course, form.getThumbnail());
        return courseRepository.save(course);
    }

    /**
     * Xóa khóa học cùng toàn bộ dữ liệu phụ thuộc (giỏ hàng, đơn hàng, đăng ký,
     * tiến độ, chứng chỉ, đánh giá, bài học, câu hỏi). Phải xóa con trước cha
     * để không vi phạm khóa ngoại trong PostgreSQL.
     */
    @Transactional
    public void delete(Long id) {
        Course course = courseRepository.findById(id).orElse(null);
        if (course == null) return;

        List<Long> lessonIds = lessonRepository.findByCourse_IdOrderByOrderIndexAsc(id)
                .stream().map(Lesson::getId).toList();
        List<Long> enrollmentIds = enrollmentRepository.findByCourse_Id(id)
                .stream().map(Enrollment::getId).toList();

        if (!lessonIds.isEmpty()) {
            // Tiến độ học + lượt làm quiz tham chiếu lesson
            quizAttemptRepository.deleteByLesson_IdIn(lessonIds);
            lessonProgressRepository.deleteByLesson_IdIn(lessonIds);
            // Xóa câu hỏi theo entity để cascade luôn các đáp án (choices)
            for (Long lessonId : lessonIds) {
                List<Question> questions = questionRepository.findByLesson_IdOrderByIdAsc(lessonId);
                if (!questions.isEmpty()) {
                    questionRepository.deleteAll(questions);
                }
            }
        }

        // Chứng chỉ gắn với enrollment của khóa học
        if (!enrollmentIds.isEmpty()) {
            certificateRepository.deleteByEnrollment_IdIn(enrollmentIds);
        }

        // Các bảng tham chiếu trực tiếp tới course
        enrollmentRepository.deleteByCourse_Id(id);
        cartItemRepository.deleteByCourse_Id(id);
        orderItemRepository.deleteByCourse_Id(id);
        reviewRepository.deleteByCourse_Id(id);

        // Cuối cùng xóa khóa học -> cascade xóa lessons (đã gỡ hết FK con)
        courseRepository.delete(course);
    }

    private void applyForm(Course course, CourseForm form) {
        course.setTitle(form.getTitle());
        course.setShortDescription(form.getShortDescription());
        // SANITIZE rich text trước khi lưu (chống XSS)
        course.setDescription(HtmlSanitizer.clean(form.getDescription()));
        course.setPrice(form.getPrice());
        course.setStatus(form.getStatus());
        if (form.getCategoryId() != null) {
            Category category = categoryRepository.findById(form.getCategoryId()).orElse(null);
            course.setCategory(category);
        }
    }

    private void uploadThumbnailIfPresent(Course course, MultipartFile file) throws IOException {
        if (file != null && !file.isEmpty()) {
            String url = cloudinaryService.upload(file, "elearning/courses");
            course.setThumbnailUrl(url);
        }
    }

    /**
     * Bảo đảm slug duy nhất (thêm hậu tố số nếu trùng).
     */
    private String uniqueSlug(String base, Long excludeId) {
        String slug = base;
        int i = 1;
        while (true) {
            var existing = courseRepository.findBySlug(slug);
            if (existing.isEmpty() || existing.get().getId().equals(excludeId)) {
                return slug;
            }
            slug = base + "-" + (++i);
        }
    }
}
