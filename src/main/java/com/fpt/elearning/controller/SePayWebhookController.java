package com.fpt.elearning.controller;

import com.fpt.elearning.dto.SePayWebhookPayload;
import com.fpt.elearning.service.PaymentService;
import com.fpt.elearning.service.SePayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Nhan webhook tu SePay khi co bien dong so du (tien vao).
 * SePay gui header: Authorization: Apikey <key>
 * Code cung chap nhan x-api-key / secure-token cho linh hoat.
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class SePayWebhookController {

    private final PaymentService paymentService;
    private final SePayService sePayService;

    @PostMapping("/api/payments/sepay/webhook")
    public ResponseEntity<Map<String, Object>> webhook(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "x-api-key", required = false) String xApiKey,
            @RequestHeader(value = "secure-token", required = false) String secureToken,
            @RequestBody SePayWebhookPayload payload) {

        // Trich key tu cac header co the co
        String apiKey = xApiKey;
        if (apiKey == null && authorization != null) {
            // Dang "Apikey <key>" hoac "Bearer <key>"
            String[] parts = authorization.trim().split("\\s+", 2);
            apiKey = parts.length == 2 ? parts[1] : authorization;
        }
        if (apiKey == null) {
            apiKey = secureToken;
        }

        if (!sePayService.isValidApiKey(apiKey)) {
            log.warn("Webhook SePay bi tu choi: API key khong hop le");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "Invalid API key"));
        }

        boolean handled = paymentService.handleSePayWebhook(payload);
        return ResponseEntity.ok(Map.of("success", handled));
    }
}
