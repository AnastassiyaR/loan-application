package com.loanapp.backend.dto;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Loan application submission request")
public class LoanApplicationRequest {

    @NotBlank
    @Size(max = 32)
    @Schema(description = "Customer first name", example = "Anna")
    private String firstName;

    @NotBlank
    @Size(max = 32)
    @Schema(description = "Customer last name", example = "Ivanova")
    private String lastName;

    @NotBlank
    @Schema(description = "Estonian personal code", example = "49503160234")
    private String personalCode;

    @NotNull
    @DecimalMin("5000.00")
    @Schema(description = "Requested loan amount", example = "15000.00")
    private BigDecimal loanAmount;

    @NotNull
    @Min(6)
    @Max(360)
    @Schema(description = "Loan period in months", example = "120")
    private Integer loanPeriodMonths;

    @NotNull
    @DecimalMin("0.0")
    @Schema(description = "Interest margin", example = "1.5")
    private BigDecimal interestMargin;
}
