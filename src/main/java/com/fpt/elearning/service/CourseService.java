package com.fpt.elearning.service;

import com.fpt.elearning.dto.CourseForm;
import com.fpt.elearning.entity.Category;
import com.fpt.elearning.entity.Course;
import com.fpt.elearning.entity.User;
import com.fpt.elearning.repository.CategoryRepository;
import com.fpt.elearning.repository.CourseRepository;
import com.fpt.elearning.util.HtmlSanitizer;
import com.fpt.elearning.util.SlugUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository courseRepository;
    private final CategoryRepository categoryRepository;
    private final CloudinaryService cloudinaryService;

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

    @Transactional
    public void delete(Long id) {
        courseRepository.deleteById(id);
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
