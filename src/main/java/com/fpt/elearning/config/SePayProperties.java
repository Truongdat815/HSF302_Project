package com.fpt.elearning.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "sepay")
@Getter
@Setter
public class SePayProperties {
    // API key cau hinh trong webhook SePay (kiem tra header Authorization: Apikey ...)
    private String webhookApiKey;
    // Tài khoản nhan tien
    private String accountNumber;
    private String accountName;
    // Ma ngan hang dung cho QR SePay (vd: MBBank, Vietcombank, ACB)
    private String bank;
    // Tien to noi dung chuyen khoan (vd: DH -> DH15)
    private String prefix;
    // Base URL tao anh QR cua SePay
    private String qrBaseUrl;
}
