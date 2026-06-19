package com.fpt.elearning.service;

import com.fpt.elearning.entity.Course;
import com.fpt.elearning.entity.Review;
import com.fpt.elearning.entity.User;
import com.fpt.elearning.repository.CourseRepository;
import com.fpt.elearning.repository.EnrollmentRepository;
import com.fpt.elearning.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;

    /**
     * Thêm/cập nhật đánh giá. Chỉ cho phép khi học viên đã sở hữu khóa học.
     */
    @Transactional
    public void addOrUpdate(User user, Long courseId, int rating, String comment) {
        if (!enrollmentRepository.existsByStudent_IdAndCourse_Id(user.getId(), courseId)) {
            throw new IllegalStateException("Bạn cần mua khóa học trước khi đánh giá");
        }
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Số sao phải từ 1 đến 5");
        }
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khóa học"));

        Review review = reviewRepository.findByCourse_IdOrderByCreatedAtDesc(courseId).stream()
                .filter(r -> r.getStudent().getId().equals(user.getId()))
                .findFirst()
                .orElseGet(() -> Review.builder().course(course).student(user).build());

        review.setRating(rating);
        review.setComment(comment);
        reviewRepository.save(review);
    }
}
