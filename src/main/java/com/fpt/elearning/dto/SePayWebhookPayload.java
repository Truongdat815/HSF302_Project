package com.fpt.elearning.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Du lieu SePay POST sang khi co bien dong so du.
 * Tham khao: https://docs.sepay.vn/tich-hop-webhooks.html
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class SePayWebhookPayload {
    private Long id;                 // ID giao dich tren SePay
    private String gateway;          // Ngân hàng, vd: MBBank
    private String transactionDate;  // Thoi gian giao dich
    private String accountNumber;    // Số tài khoản nhan
    private String code;             // Ma code SePay tu nhan dien (neu cau hinh)
    private String content;          // Nội dung chuyen khoan
    private String transferType;     // "in" = tien vao, "out" = tien ra
    private BigDecimal transferAmount; // Số tiền giao dich
    private BigDecimal accumulated;  // So du tich luy
    private String subAccount;       // Tài khoản phu (neu co)
    private String referenceCode;    // Ma tham chieu (SMS reference)
    private String description;      // Mô tả toan van
}
