package com.fpt.elearning.controller;

import com.fpt.elearning.entity.Cart;
import com.fpt.elearning.entity.Coupon;
import com.fpt.elearning.entity.Order;
import com.fpt.elearning.entity.User;
import com.fpt.elearning.service.AuthHelper;
import com.fpt.elearning.service.CartService;
import com.fpt.elearning.service.CouponService;
import com.fpt.elearning.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.Optional;

@Controller
@RequestMapping("/checkout")
@RequiredArgsConstructor
public class CheckoutController {

    private final CartService cartService;
    private final CouponService couponService;
    private final OrderService orderService;
    private final AuthHelper authHelper;

    @GetMapping
    public String checkout(@RequestParam(required = false) String coupon, Model model) {
        User user = authHelper.requireCurrentUser();
        Cart cart = cartService.getOrCreateCart(user);
        BigDecimal subtotal = cartService.subtotal(cart);

        Optional<Coupon> couponOpt = couponService.validate(coupon);
        BigDecimal discount = couponService.computeDiscount(couponOpt.orElse(null), subtotal);
        BigDecimal total = subtotal.subtract(discount).max(BigDecimal.ZERO);

        model.addAttribute("cart", cart);
        model.addAttribute("subtotal", subtotal);
        model.addAttribute("discount", discount);
        model.addAttribute("total", total);
        model.addAttribute("couponCode", coupon);
        model.addAttribute("couponValid", couponOpt.isPresent());
        model.addAttribute("couponInvalid", coupon != null && !coupon.isBlank() && couponOpt.isEmpty());
        return "checkout";
    }

    @PostMapping("/place-order")
    public String placeOrder(@RequestParam(required = false) String couponCode,
                             RedirectAttributes ra) {
        User user = authHelper.requireCurrentUser();
        try {
            Order order = orderService.createOrderFromCart(user, couponCode);
            // Chuyen sang trang QR de chuyen khoan qua SePay
            return "redirect:/payment/" + order.getId();
        } catch (IllegalStateException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
            return "redirect:/cart";
        }
    }
}
