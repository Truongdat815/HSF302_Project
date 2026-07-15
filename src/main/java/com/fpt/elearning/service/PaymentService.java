package com.fpt.elearning.service;

import com.fpt.elearning.dto.SePayWebhookPayload;
import com.fpt.elearning.entity.*;
import com.fpt.elearning.entity.enums.OrderStatus;
import com.fpt.elearning.entity.enums.PaymentStatus;
import com.fpt.elearning.repository.CouponRepository;
import com.fpt.elearning.repository.EnrollmentRepository;
import com.fpt.elearning.repository.OrderRepository;
import com.fpt.elearning.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CouponRepository couponRepository;
    private final SePayService sePayService;
    private final CartService cartService;

    /**
     * Xử lý webhook SePay (biến động số dư). Trả về true nếu đã ghi nhận thanh toán.
     */
    @Transactional
    public boolean handleSePayWebhook(SePayWebhookPayload payload) {
        // Chi xu ly tien VAO
        if (payload.getTransferType() != null && !"in".equalsIgnoreCase(payload.getTransferType())) {
            log.info("Bỏ qua giao dịch không phải tiền vào: {}", payload.getTransferType());
            return false;
        }

        // Trich orderId tu cac field SePay co the gui ma thanh toan (vd: DH15).
        Long orderId = sePayService.extractOrderId(payload.getContent());
        if (orderId == null) {
            orderId = sePayService.extractOrderId(payload.getDescription());
        }
        if (orderId == null) {
            orderId = sePayService.extractOrderId(payload.getCode());
        }
        if (orderId == null) {
            log.warn("Khong tim thay ma don hang trong webhook SePay: content={}, code={}, description={}",
                    payload.getContent(), payload.getCode(), payload.getDescription());
            return false;
        }

        Payment payment = paymentRepository.findByOrder_Id(orderId).orElse(null);
        if (payment == null) {
            log.warn("Không tìm thấy thanh toán cho đơn #{}", orderId);
            return false;
        }

        // Idempotent: đã xử lý trước đó
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            return true;
        }

        // Kiểm tra số tiền (cho phép chuyển dư hoặc đúng)
        BigDecimal received = payload.getTransferAmount() != null ? payload.getTransferAmount() : BigDecimal.ZERO;
        if (received.compareTo(payment.getAmount()) < 0) {
            log.warn("Số tiền chuyển ({}) ít hơn đơn #{} ({})", received, orderId, payment.getAmount());
            return false;
        }

        // ===== Ghi nhan thanh cong =====
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setPaymentDate(LocalDateTime.now());
        payment.setReferenceCode(payload.getReferenceCode());
        payment.setGateway(payload.getGateway());

        Order order = payment.getOrder();
        order.setStatus(OrderStatus.PAID);

        // Tạo Enrollment cho từng khóa học trong đơn
        for (OrderItem item : order.getItems()) {
            boolean already = enrollmentRepository
                    .existsByStudent_IdAndCourse_Id(order.getUser().getId(), item.getCourse().getId());
            if (!already) {
                enrollmentRepository.save(Enrollment.builder()
                        .student(order.getUser())
                        .course(item.getCourse())
                        .progress(0)
                        .build());
            }
        }

        // Tang so lan dung coupon
        Coupon coupon = order.getCoupon();
        if (coupon != null) {
            coupon.setUsedCount(coupon.getUsedCount() + 1);
            couponRepository.save(coupon);
        }

        // Xóa giỏ hàng
        cartService.clearCart(order.getUser());

        orderRepository.save(order);
        paymentRepository.save(payment);
        log.info("Đã ghi nhận thanh toán đơn #{} ({} VND)", orderId, received);
        return true;
    }
}
