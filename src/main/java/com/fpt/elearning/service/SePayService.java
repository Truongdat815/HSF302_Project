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
     * Tao link anh QR SePay (VietQR) co san so tien + noi dung.
     * VD: https://qr.sepay.vn/img?acc=0968097907&bank=MBBank&amount=200000&des=DH15
     */
    public String buildQrUrl(BigDecimal amount, String content) {
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
}
