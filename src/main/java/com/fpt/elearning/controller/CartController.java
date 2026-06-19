package com.fpt.elearning.controller;

import com.fpt.elearning.entity.Cart;
import com.fpt.elearning.entity.User;
import com.fpt.elearning.service.AuthHelper;
import com.fpt.elearning.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;
    private final AuthHelper authHelper;

    @GetMapping("/cart")
    public String viewCart(Model model) {
        User user = authHelper.requireCurrentUser();
        Cart cart = cartService.getOrCreateCart(user);
        model.addAttribute("cart", cart);
        model.addAttribute("subtotal", cartService.subtotal(cart));
        return "cart";
    }

    @PostMapping("/cart/add")
    public String add(@RequestParam Long courseId, RedirectAttributes ra) {
        User user = authHelper.requireCurrentUser();
        String error = cartService.addCourse(user, courseId);
        if (error != null) {
            ra.addFlashAttribute("error", error);
        } else {
            ra.addFlashAttribute("success", "Đã thêm vào giỏ hàng.");
        }
        return "redirect:/cart";
    }

    @PostMapping("/cart/remove")
    public String remove(@RequestParam Long courseId, RedirectAttributes ra) {
        User user = authHelper.requireCurrentUser();
        cartService.removeCourse(user, courseId);
        ra.addFlashAttribute("success", "Đã xóa khỏi giỏ.");
        return "redirect:/cart";
    }
}
