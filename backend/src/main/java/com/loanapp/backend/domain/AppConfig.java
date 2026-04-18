package com.loanapp.backend.domain;


import jakarta.persistence.Column;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "app_config")
public class AppConfig {

    @Id
    private Long id;

    @Column(name = "max_customer_age", nullable = false)
    private Integer maxCustomerAge;

    @Column(name = "base_interest_rate", nullable = false, precision = 6, scale = 4)
    private BigDecimal baseInterestRate;
}
