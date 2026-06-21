package com.fpt.elearning.repository;

import com.fpt.elearning.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    void deleteByCourse_Id(Long courseId);
}
