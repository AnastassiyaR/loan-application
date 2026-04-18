package com.loanapp.backend.dto;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Current dynamic application configuration")
public class AppConfigDto {

    @Schema(description = "Maximum allowed customer age", example = "70")
    private Integer maxCustomerAge;

    @Schema(description = "Base interest rate (Euribor 6m)", example = "1.234")
    private BigDecimal baseInterestRate;
}
