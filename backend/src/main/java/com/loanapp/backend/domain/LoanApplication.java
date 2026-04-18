package com.loanapp.backend.domain;


import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "loan_application")
public class LoanApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "first_name", nullable = false, length = 32)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 32)
    private String lastName;

    @Column(name = "personal_code", nullable = false, length = 11)
    private String personalCode;

    @Column(name = "loan_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal loanAmount;

    @Column(name = "loan_period_months", nullable = false)
    private Integer loanPeriodMonths;

    @Column(name = "interest_margin", nullable = false, precision = 6, scale = 4)
    private BigDecimal interestMargin;

    @Column(name = "base_interest_rate", nullable = false, precision = 6, scale = 4)
    private BigDecimal baseInterestRate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private LoanStatus status = LoanStatus.STARTED;

    @Enumerated(EnumType.STRING)
    @Column(name = "rejection_reason")
    private RejectionReason rejectionReason;

    @OneToMany(mappedBy = "loanApplication", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("paymentNumber ASC")
    private List<PaymentSchedule> paymentSchedule = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    private void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    private void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
