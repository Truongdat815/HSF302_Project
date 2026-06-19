package com.fpt.elearning.service;

import com.fpt.elearning.entity.Cart;
import com.fpt.elearning.entity.CartItem;
import com.fpt.elearning.entity.Course;
import com.fpt.elearning.entity.User;
import com.fpt.elearning.repository.CartRepository;
import com.fpt.elearning.repository.CourseRepository;
import com.fpt.elearning.repository.EnrollmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;

    @Transactional
    public Cart getOrCreateCart(User user) {
        return cartRepository.findByUser_Id(user.getId())
                .orElseGet(() -> cartRepository.save(Cart.builder().user(user).build()));
    }

    /**
     * Thêm khóa học vao gio. Tra ve thong bao loi (null neu thanh cong).
     */
    @Transactional
    public String addCourse(User user, Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khóa học"));

        if (enrollmentRepository.existsByStudent_IdAndCourse_Id(user.getId(), courseId)) {
            return "Bạn đã sở hữu khóa học này.";
        }

        Cart cart = getOrCreateCart(user);
        boolean exists = cart.getItems().stream()
                .anyMatch(i -> i.getCourse().getId().equals(courseId));
        if (exists) {
            return "Khóa học đã có trong giỏ.";
        }

        CartItem item = CartItem.builder().cart(cart).course(course).build();
        cart.getItems().add(item);
        cartRepository.save(cart);
        return null;
    }

    @Transactional
    public void removeCourse(User user, Long courseId) {
        Cart cart = getOrCreateCart(user);
        cart.getItems().removeIf(i -> i.getCourse().getId().equals(courseId));
        cartRepository.save(cart);
    }

    @Transactional
    public void clearCart(User user) {
        Cart cart = getOrCreateCart(user);
        cart.getItems().clear();
        cartRepository.save(cart);
    }

    @Transactional(readOnly = true)
    public BigDecimal subtotal(Cart cart) {
        return cart.getItems().stream()
                .map(i -> i.getCourse().getPrice())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
