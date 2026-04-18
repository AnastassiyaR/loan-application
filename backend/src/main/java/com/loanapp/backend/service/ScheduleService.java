package com.loanapp.backend.service;


import com.loanapp.backend.exception.BusinessException;
import com.loanapp.backend.exception.ErrorCode;
import com.loanapp.backend.domain.LoanApplication;
import com.loanapp.backend.domain.LoanStatus;
import com.loanapp.backend.domain.PaymentSchedule;
import com.loanapp.backend.repository.LoanApplicationRepository;
import com.loanapp.backend.repository.PaymentScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final PaymentScheduleRepository scheduleRepository;
    private final LoanApplicationRepository loanRepository;
    private final ConfigService configService;

    public void generateSchedule(LoanApplication loan) {

        if (loan.getStatus() != LoanStatus.STARTED) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION);
        }

        generateScheduleInternal(loan);

        loan.setStatus(LoanStatus.IN_REVIEW);
        loanRepository.save(loan);
    }

    public void generateScheduleInternal(LoanApplication loan) {

        scheduleRepository.deleteByLoanApplicationId(loan.getId());

        BigDecimal annualRate = configService.getConfig().getBaseInterestRate().add(loan.getInterestMargin());
        BigDecimal monthlyRate = annualRate.divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_UP);

        int months = loan.getLoanPeriodMonths();
        BigDecimal principal = loan.getLoanAmount();

        BigDecimal numerator = principal.multiply(monthlyRate);
        BigDecimal denominator = BigDecimal.ONE.subtract(
                BigDecimal.ONE.add(monthlyRate).pow(-months, java.math.MathContext.DECIMAL64)
        );
        BigDecimal monthlyPayment = numerator.divide(denominator, 2, RoundingMode.HALF_UP);

        LocalDate paymentDate = LocalDate.now();
        BigDecimal remaining = principal;

        for (int i = 1; i <= months; i++) {

            BigDecimal interest = remaining.multiply(monthlyRate).setScale(2, RoundingMode.HALF_UP);
            BigDecimal principalPart = monthlyPayment.subtract(interest).setScale(2, RoundingMode.HALF_UP);

            remaining = remaining.subtract(principalPart);

            PaymentSchedule item = PaymentSchedule.builder()
                    .loanApplication(loan)
                    .paymentNumber(i)
                    .paymentDate(paymentDate)
                    .principalAmount(principalPart)
                    .interestAmount(interest)
                    .totalPayment(monthlyPayment)
                    .remainingBalance(remaining)
                    .build();

            scheduleRepository.save(item);

            paymentDate = paymentDate.plusMonths(1);
        }
    }
}
