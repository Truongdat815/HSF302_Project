package com.fpt.elearning.service;

import com.fpt.elearning.entity.Coupon;
import com.fpt.elearning.entity.enums.DiscountType;
import com.fpt.elearning.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;

    /**
     * Kiem tra ma hop le. Tra ve Coupon neu hop le, rong neu khong.
     */
    public Optional<Coupon> validate(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        return couponRepository.findByCodeIgnoreCase(code.trim())
                .filter(Coupon::isActive)
                .filter(c -> c.getExpiryDate() == null || c.getExpiryDate().isAfter(LocalDateTime.now()))
                .filter(c -> c.getUsageLimit() == null || c.getUsedCount() < c.getUsageLimit());
    }

    /**
     * Tinh so tien duoc giam (khong vuot qua subtotal).
     */
    public BigDecimal computeDiscount(Coupon coupon, BigDecimal subtotal) {
        if (coupon == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal discount;
        if (coupon.getDiscountType() == DiscountType.PERCENT) {
            discount = subtotal.multiply(coupon.getValue())
                    .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
        } else {
            discount = coupon.getValue();
        }
        return discount.min(subtotal).max(BigDecimal.ZERO);
    }
}
