package com.loanapp.backend.dto;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to regenerate payment schedule")
public class ScheduleRegenerateRequest {

    @NotNull
    @DecimalMin("0.0")
    @Schema(description = "Interest margin", example = "1.5")
    private BigDecimal interestMargin;

    @NotNull
    @Min(6)
    @Max(360)
    @Schema(description = "Loan period in months", example = "120")
    private Integer loanPeriodMonths;
}
