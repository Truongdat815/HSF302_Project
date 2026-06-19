package com.fpt.elearning.controller;

import com.fpt.elearning.config.SePayProperties;
import com.fpt.elearning.entity.Order;
import com.fpt.elearning.entity.Payment;
import com.fpt.elearning.entity.User;
import com.fpt.elearning.repository.OrderRepository;
import com.fpt.elearning.repository.PaymentRepository;
import com.fpt.elearning.service.AuthHelper;
import com.fpt.elearning.service.SePayService;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.stereotype.Controller;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

@Controller
@RequiredArgsConstructor
public class PaymentController {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final SePayService sePayService;
    private final SePayProperties sePayProperties;
    private final AuthHelper authHelper;

    /**
     * Trang QR chuyển khoản cho đơn hàng.
     */
    @GetMapping("/payment/{orderId}")
    @Transactional(readOnly = true)
    public String paymentPage(@PathVariable Long orderId, Model model, RedirectAttributes ra) {
        User user = authHelper.requireCurrentUser();
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null || !order.getUser().getId().equals(user.getId())) {
            ra.addFlashAttribute("error", "Không tìm thấy đơn hàng.");
            return "redirect:/cart";
        }
        Payment payment = paymentRepository.findByOrder_Id(orderId).orElse(null);
        if (payment == null) {
            ra.addFlashAttribute("error", "Không tìm thấy thanh toán.");
            return "redirect:/cart";
        }

        model.addAttribute("order", order);
        model.addAttribute("payment", payment);
        model.addAttribute("qrUrl", sePayService.buildQrUrl(payment.getAmount(), payment.getTransferCode()));
        model.addAttribute("accountNumber", sePayProperties.getAccountNumber());
        model.addAttribute("accountName", sePayProperties.getAccountName());
        model.addAttribute("bank", sePayProperties.getBank());
        return "payment/qr";
    }

    /**
     * Endpoint cho trang QR poll trạng thái thanh toán (trả JSON).
     */
    @GetMapping("/payment/{orderId}/status")
    @ResponseBody
    @Transactional(readOnly = true)
    public Map<String, String> status(@PathVariable Long orderId) {
        User user = authHelper.requireCurrentUser();
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null || !order.getUser().getId().equals(user.getId())) {
            return Map.of("status", "NOT_FOUND");
        }
        Payment payment = paymentRepository.findByOrder_Id(orderId).orElse(null);
        return Map.of("status", payment != null ? payment.getStatus().name() : "PENDING");
    }
}
