package com.fpt.elearning.repository;

import com.fpt.elearning.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    boolean existsByCart_IdAndCourse_Id(Long cartId, Long courseId);
    void deleteByCart_IdAndCourse_Id(Long cartId, Long courseId);
    void deleteByCourse_Id(Long courseId);
}
