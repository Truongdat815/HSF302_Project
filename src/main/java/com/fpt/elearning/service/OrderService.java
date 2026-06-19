package com.fpt.elearning.service;

import com.fpt.elearning.entity.*;
import com.fpt.elearning.entity.enums.OrderStatus;
import com.fpt.elearning.entity.enums.PaymentStatus;
import com.fpt.elearning.repository.OrderRepository;
import com.fpt.elearning.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final CartService cartService;
    private final CouponService couponService;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final SePayService sePayService;

    /**
     * Tao Order (PENDING) tu gio hang + Payment (PENDING) di kem.
     */
    @Transactional
    public Order createOrderFromCart(User user, String couponCode) {
        Cart cart = cartService.getOrCreateCart(user);
        if (cart.getItems().isEmpty()) {
            throw new IllegalStateException("Giỏ hàng dang trong");
        }

        BigDecimal subtotal = cartService.subtotal(cart);
        Optional<Coupon> couponOpt = couponService.validate(couponCode);
        Coupon coupon = couponOpt.orElse(null);
        BigDecimal discount = couponService.computeDiscount(coupon, subtotal);
        BigDecimal total = subtotal.subtract(discount).max(BigDecimal.ZERO);

        Order order = Order.builder()
                .user(user)
                .subTotal(subtotal)
                .discountAmount(discount)
                .totalAmount(total)
                .coupon(coupon)
                .status(OrderStatus.PENDING)
                .build();

        cart.getItems().forEach(item -> {
            OrderItem oi = OrderItem.builder()
                    .order(order)
                    .course(item.getCourse())
                    .price(item.getCourse().getPrice())
                    .build();
            order.getItems().add(oi);
        });

        Order saved = orderRepository.save(order);

        Payment payment = Payment.builder()
                .order(saved)
                .amount(total)
                // Ma noi dung chuyen khoan gan voi don hang, vd: DH15
                .transferCode(sePayService.transferCode(saved.getId()))
                .status(PaymentStatus.PENDING)
                .build();
        paymentRepository.save(payment);

        return saved;
    }
}
