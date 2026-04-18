package com.loanapp.backend.service;


import com.loanapp.backend.domain.LoanApplication;
import com.loanapp.backend.domain.LoanStatus;
import com.loanapp.backend.domain.PaymentSchedule;
import com.loanapp.backend.dto.AppConfigDto;
import com.loanapp.backend.exception.BusinessException;
import com.loanapp.backend.exception.ErrorCode;
import com.loanapp.backend.repository.LoanApplicationRepository;
import com.loanapp.backend.repository.PaymentScheduleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScheduleServiceTest {

    @Mock private PaymentScheduleRepository scheduleRepository;
    @Mock private LoanApplicationRepository loanRepository;
    @Mock private ConfigService configService;

    @InjectMocks
    private ScheduleService scheduleService;

    private AppConfigDto config;

    @BeforeEach
    void setUp() {
        config = new AppConfigDto();
        config.setBaseInterestRate(new BigDecimal("0.0390")); // 3.90% annual
        config.setMaxCustomerAge(70);
    }


    @Nested
    @DisplayName("generateSchedule")
    class GenerateSchedule {

        @Test
        @DisplayName("if STARTED loan, then generates schedule and transitions to IN_REVIEW")
        void startedLoan_generatesAndTransitions() {
            LoanApplication loan = buildLoan(LoanStatus.STARTED, new BigDecimal("10000.00"), 12,
                    new BigDecimal("0.0110"));
            when(configService.getConfig()).thenReturn(config);

            scheduleService.generateSchedule(loan);

            verify(scheduleRepository, atLeast(12)).save(any(PaymentSchedule.class));
            verify(loanRepository).save(loan);
            assertThat(loan.getStatus()).isEqualTo(LoanStatus.IN_REVIEW);
        }

        @Test
        @DisplayName("if non-STARTED loan, then throws BusinessException INVALID_STATUS_TRANSITION")
        void nonStartedLoan_throwsException() {
            LoanApplication loan = buildLoan(LoanStatus.IN_REVIEW, new BigDecimal("10000.00"), 12,
                    new BigDecimal("0.0110"));

            assertThatThrownBy(() -> scheduleService.generateSchedule(loan))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_STATUS_TRANSITION);

            verifyNoInteractions(scheduleRepository, loanRepository, configService);
        }
    }


    @Nested
    @DisplayName("generateScheduleInternal - amortisation math")
    class GenerateScheduleInternal {

        @Test
        @DisplayName("saves exactly N payment entries for N-month loan")
        void savesCorrectNumberOfPayments() {
            LoanApplication loan = buildLoan(LoanStatus.IN_REVIEW, new BigDecimal("15000.00"), 24,
                    new BigDecimal("0.0150"));
            when(configService.getConfig()).thenReturn(config);

            scheduleService.generateScheduleInternal(loan);

            verify(scheduleRepository, times(24)).save(any(PaymentSchedule.class));
        }

        @Test
        @DisplayName("payment numbers run from 1 to N sequentially")
        void paymentNumbersAreSequential() {
            int months = 6;
            LoanApplication loan = buildLoan(LoanStatus.IN_REVIEW, new BigDecimal("5000.00"), months,
                    new BigDecimal("0.0200"));
            when(configService.getConfig()).thenReturn(config);

            ArgumentCaptor<PaymentSchedule> captor = ArgumentCaptor.forClass(PaymentSchedule.class);
            scheduleService.generateScheduleInternal(loan);
            verify(scheduleRepository, times(months)).save(captor.capture());

            List<PaymentSchedule> items = captor.getAllValues();
            for (int i = 0; i < months; i++) {
                assertThat(items.get(i).getPaymentNumber()).isEqualTo(i + 1);
            }
        }

        @Test
        @DisplayName("payment dates advance monthly from today")
        void paymentDatesAdvanceMonthly() {
            int months = 3;
            LoanApplication loan = buildLoan(LoanStatus.IN_REVIEW, new BigDecimal("5000.00"), months,
                    new BigDecimal("0.0200"));
            when(configService.getConfig()).thenReturn(config);

            ArgumentCaptor<PaymentSchedule> captor = ArgumentCaptor.forClass(PaymentSchedule.class);
            scheduleService.generateScheduleInternal(loan);
            verify(scheduleRepository, times(months)).save(captor.capture());

            List<PaymentSchedule> items = captor.getAllValues();
            LocalDate today = LocalDate.now();
            assertThat(items.get(0).getPaymentDate()).isEqualTo(today);
            assertThat(items.get(1).getPaymentDate()).isEqualTo(today.plusMonths(1));
            assertThat(items.get(2).getPaymentDate()).isEqualTo(today.plusMonths(2));
        }

        @Test
        @DisplayName("totalPayment = principalPart + interestPart for each row")
        void totalPaymentEqualsComponentSum() {
            LoanApplication loan = buildLoan(LoanStatus.IN_REVIEW, new BigDecimal("10000.00"), 12,
                    new BigDecimal("0.0110"));
            when(configService.getConfig()).thenReturn(config);

            ArgumentCaptor<PaymentSchedule> captor = ArgumentCaptor.forClass(PaymentSchedule.class);
            scheduleService.generateScheduleInternal(loan);
            verify(scheduleRepository, times(12)).save(captor.capture());

            captor.getAllValues().forEach(item -> {
                BigDecimal expected = item.getPrincipalAmount().add(item.getInterestAmount());
                // Allow 1 cent rounding tolerance
                assertThat(item.getTotalPayment().subtract(expected).abs())
                        .isLessThanOrEqualTo(new BigDecimal("0.01"));
            });
        }

        @Test
        @DisplayName("remaining balance decreases monotonically")
        void remainingBalanceDecreases() {
            LoanApplication loan = buildLoan(LoanStatus.IN_REVIEW, new BigDecimal("20000.00"), 24,
                    new BigDecimal("0.0150"));
            when(configService.getConfig()).thenReturn(config);

            ArgumentCaptor<PaymentSchedule> captor = ArgumentCaptor.forClass(PaymentSchedule.class);
            scheduleService.generateScheduleInternal(loan);
            verify(scheduleRepository, times(24)).save(captor.capture());

            List<PaymentSchedule> items = captor.getAllValues();
            for (int i = 1; i < items.size(); i++) {
                assertThat(items.get(i).getRemainingBalance())
                        .isLessThan(items.get(i - 1).getRemainingBalance());
            }
        }

        @Test
        @DisplayName("if higher interest margin, then increase monthly payment")
        void higherMarginIncreasesPayment() {
            BigDecimal principal = new BigDecimal("10000.00");
            int months = 12;

            LoanApplication loanLow = buildLoan(LoanStatus.IN_REVIEW, principal, months,
                    new BigDecimal("0.0100"));
            LoanApplication loanHigh = buildLoan(LoanStatus.IN_REVIEW, principal, months,
                    new BigDecimal("0.0500"));

            when(configService.getConfig()).thenReturn(config);

            ArgumentCaptor<PaymentSchedule> captorLow = ArgumentCaptor.forClass(PaymentSchedule.class);
            scheduleService.generateScheduleInternal(loanLow);
            verify(scheduleRepository, times(months)).save(captorLow.capture());
            BigDecimal paymentLow = captorLow.getAllValues().getFirst().getTotalPayment();

            reset(scheduleRepository);
            when(configService.getConfig()).thenReturn(config);

            ArgumentCaptor<PaymentSchedule> captorHigh = ArgumentCaptor.forClass(PaymentSchedule.class);
            scheduleService.generateScheduleInternal(loanHigh);
            verify(scheduleRepository, times(months)).save(captorHigh.capture());
            BigDecimal paymentHigh = captorHigh.getAllValues().getFirst().getTotalPayment();

            assertThat(paymentHigh).isGreaterThan(paymentLow);
        }
    }

    private LoanApplication buildLoan(LoanStatus status, BigDecimal amount, int months,
                                      BigDecimal margin) {
        return LoanApplication.builder()
                .id(1L)
                .status(status)
                .loanAmount(amount)
                .loanPeriodMonths(months)
                .interestMargin(margin)
                .baseInterestRate(BigDecimal.ZERO)
                .build();
    }
}
