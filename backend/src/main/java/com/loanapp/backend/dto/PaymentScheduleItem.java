package com.loanapp.backend.dto;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Single payment schedule entry")
public class PaymentScheduleItem {

    @Schema(description = "Payment schedule row ID", example = "10")
    private Long id;

    @Schema(description = "Payment date", example = "2025-05-01")
    private LocalDate paymentDate;

    @Schema(description = "Principal part of payment", example = "120.50")
    private BigDecimal principalAmount;

    @Schema(description = "Interest part of payment", example = "30.20")
    private BigDecimal interestAmount;

    @Schema(description = "Total payment amount", example = "150.70")
    private BigDecimal totalPayment;

    @Schema(description = "Remaining loan balance", example = "14879.50")
    private BigDecimal remainingBalance;
}
