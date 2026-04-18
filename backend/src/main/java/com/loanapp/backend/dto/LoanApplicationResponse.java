package com.loanapp.backend.dto;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.loanapp.backend.domain.LoanStatus;
import com.loanapp.backend.domain.RejectionReason;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Loan application details with schedule")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LoanApplicationResponse {

    @Schema(description = "Loan application ID", example = "1")
    private Long id;

    @Schema(description = "Customer first name", example = "Anna")
    private String firstName;

    @Schema(description = "Customer last name", example = "Ivanova")
    private String lastName;

    @Schema(description = "Estonian personal code", example = "49503160234")
    private String personalCode;

    @Schema(description = "Loan amount", example = "15000.00")
    private BigDecimal loanAmount;

    @Schema(description = "Loan period in months", example = "120")
    private Integer loanPeriodMonths;

    @Schema(description = "Interest margin", example = "1.5")
    private BigDecimal interestMargin;

    @Schema(description = "Current loan status")
    private LoanStatus status;

    @Schema(description = "Reason for rejection, if rejected")
    private RejectionReason rejectionReason;

    @Schema(description = "Creation timestamp")
    private LocalDateTime createdAt;

    @Schema(description = "Generated payment schedule")
    private List<PaymentScheduleItem> paymentSchedule;
}
