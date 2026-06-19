package com.fpt.elearning.entity;

import com.fpt.elearning.entity.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    // Ma noi dung chuyen khoan (vd: DH15) - khop voi noi dung CK tu SePay - duy nhat
    @Column(nullable = false, unique = true, length = 50)
    private String transferCode;

    // Ma giao dich ngan hang SePay tra ve (referenceCode)
    private String referenceCode;

    // Cong/ngan hang (gateway) tu webhook SePay, vd: MBBank
    private String gateway;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.PENDING;

    private LocalDateTime paymentDate;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
