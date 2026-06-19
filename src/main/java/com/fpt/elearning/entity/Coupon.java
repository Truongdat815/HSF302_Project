package com.fpt.elearning.entity;

import com.fpt.elearning.entity.enums.DiscountType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "coupons")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DiscountType discountType;

    // PERCENT: 0-100 ; FIXED: so tien VND
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal value;

    private LocalDateTime expiryDate;

    // So lan toi da duoc dung (null = khong gioi han)
    private Integer usageLimit;

    @Column(nullable = false)
    @Builder.Default
    private Integer usedCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;
}
