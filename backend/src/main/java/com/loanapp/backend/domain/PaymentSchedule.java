package com.loanapp.backend.domain;


import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Builder
@Getter
@Setter
@Table(name = "payment_schedule")
@NoArgsConstructor
@AllArgsConstructor
public class PaymentSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_application_id", nullable = false)
    private LoanApplication loanApplication;

    @Column(name = "payment_number", nullable = false)
    private Integer paymentNumber;

    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    @Column(name = "principal_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal principalAmount;

    @Column(name = "interest_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal interestAmount;

    @Column(name = "total_payment", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalPayment;

    @Column(name = "remaining_balance", nullable = false, precision = 15, scale = 2)
    private BigDecimal remainingBalance;
}
