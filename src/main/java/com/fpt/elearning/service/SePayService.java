package com.fpt.elearning.service;

import com.fpt.elearning.config.SePayProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tien ich tich hop SePay: sinh ma noi dung chuyen khoan, tao link QR,
 * trich orderId tu noi dung va xac thuc API key webhook.
 */
@Service
@RequiredArgsConstructor
public class SePayService {

    private final SePayProperties props;

    /**
     * Ma noi dung chuyen khoan, vd: DH15
     */
    public String transferCode(Long orderId) {
        return props.getPrefix() + orderId;
    }

    /**
     * Tao anh VietQR co san so tien + noi dung. Webhook van do SePay xu ly,
     * con QR nen dung dinh dang VietQR chuan de app ngan hang quet on dinh hon.
     * VD: https://img.vietqr.io/image/MB-0968097907-compact2.png?amount=200000&addInfo=DH15
     */
    public String buildQrUrl(BigDecimal amount, String content) {
        long amountVnd = amount.setScale(0, java.math.RoundingMode.HALF_UP).longValueExact();
        return "https://img.vietqr.io/image/" + encPath(bankCode(props.getBank()))
                + "-" + encPath(props.getAccountNumber())
                + "-compact2.png"
                + "?amount=" + amountVnd
                + "&addInfo=" + enc(content)
                + "&accountName=" + enc(props.getAccountName());
    }

    /**
     * SePay config hien thi ten ngan hang, trong khi VietQR can ma ngan hang.
     */
    private String bankCode(String bank) {
        if (bank == null || bank.isBlank()) {
            return "";
        }
        String normalized = bank.replaceAll("[\\s_-]+", "").toUpperCase();
        if (normalized.equals("MBBANK") || normalized.equals("MB")) {
            return "MB";
        }
        return bank;
    }

    public String buildSePayQrUrl(BigDecimal amount, String content) {
        long amountVnd = amount.setScale(0, java.math.RoundingMode.HALF_UP).longValueExact();
        return props.getQrBaseUrl()
                + "?acc=" + enc(props.getAccountNumber())
                + "&bank=" + enc(props.getBank())
                + "&amount=" + amountVnd
                + "&des=" + enc(content);
    }

    /**
     * Trich orderId tu noi dung chuyen khoan (tim chuoi PREFIX + so).
     * VD content = "CT DEN:986... DH15" -> 15
     */
    public Long extractOrderId(String content) {
        if (content == null) {
            return null;
        }
        Pattern p = Pattern.compile("(?i)" + Pattern.quote(props.getPrefix()) + "0*(\\d+)");
        Matcher m = p.matcher(content);
        if (m.find()) {
            try {
                return Long.parseLong(m.group(1));
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    /**
     * Kiem tra API key tu header webhook (Authorization: Apikey ..., x-api-key, secure-token).
     */
    public boolean isValidApiKey(String provided) {
        return provided != null && provided.equals(props.getWebhookApiKey());
    }

    private String enc(String v) {
        return URLEncoder.encode(v == null ? "" : v, StandardCharsets.UTF_8);
    }

    private String encPath(String v) {
        return enc(v).replace("+", "%20");
    }
}
