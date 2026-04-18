package com.loanapp.backend.service;


import com.loanapp.backend.domain.LoanApplication;
import com.loanapp.backend.domain.LoanStatus;
import com.loanapp.backend.domain.RejectionReason;
import com.loanapp.backend.dto.*;
import com.loanapp.backend.exception.BusinessException;
import com.loanapp.backend.exception.ErrorCode;
import com.loanapp.backend.mapper.LoanApplicationMapper;
import com.loanapp.backend.mapper.PaymentScheduleMapper;
import com.loanapp.backend.repository.LoanApplicationRepository;
import com.loanapp.backend.repository.PaymentScheduleRepository;
import com.loanapp.backend.validation.EstonianPersonalCodeValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoanServiceTest {

    @Mock private LoanApplicationRepository loanRepository;
    @Mock private ScheduleService scheduleService;
    @Mock private EstonianPersonalCodeValidator personalCodeValidator;
    @Mock private ConfigService configService;
    @Mock private LoanApplicationMapper loanApplicationMapper;
    @Mock private PaymentScheduleRepository scheduleRepository;
    @Mock private PaymentScheduleMapper paymentScheduleMapper;


    @InjectMocks
    private LoanService loanService;

    // Shared test fixtures
    private static final String VALID_CODE = "37605030299";
    private static final BigDecimal LOAN_AMOUNT = new BigDecimal("15000.00");
    private static final int LOAN_PERIOD = 120;

    private LoanApplicationRequest validRequest;
    private AppConfigDto config;

    @BeforeEach
    void setUp() {
        validRequest = new LoanApplicationRequest();
        validRequest.setPersonalCode(VALID_CODE);
        validRequest.setFirstName("Anna");
        validRequest.setLastName("Ivanova");
        validRequest.setLoanAmount(LOAN_AMOUNT);
        validRequest.setLoanPeriodMonths(LOAN_PERIOD);
        validRequest.setInterestMargin(new BigDecimal("0.0150"));

        config = new AppConfigDto();
        config.setMaxCustomerAge(70);
        config.setBaseInterestRate(new BigDecimal("0.0390"));
    }


    @Nested
    @DisplayName("submitApplication")
    class SubmitApplication {

        @Test
        @DisplayName("if valid request under age limit, then saves with STARTED, generates schedule")
        void happyPath_createsStartedApplication() {
            when(personalCodeValidator.isValid(VALID_CODE)).thenReturn(true);
            when(loanRepository.existsByPersonalCodeAndStatusIn(eq(VALID_CODE), anySet()))
                    .thenReturn(false);
            when(personalCodeValidator.extractBirthDate(VALID_CODE))
                    .thenReturn(LocalDate.of(1995, 3, 16));
            when(configService.getConfig()).thenReturn(config);

            LoanApplication entity = LoanApplication.builder()
                    .personalCode(VALID_CODE)
                    .status(LoanStatus.STARTED)
                    .build();
            when(loanApplicationMapper.toEntity(validRequest)).thenReturn(entity);
            when(loanRepository.save(entity)).thenReturn(entity);

            LoanApplicationResponse expectedResponse = LoanApplicationResponse.builder()
                    .status(LoanStatus.STARTED)
                    .build();
            when(loanApplicationMapper.toDto(entity)).thenReturn(expectedResponse);

            LoanApplicationResponse result = loanService.submitApplication(validRequest);

            assertThat(result.getStatus()).isEqualTo(LoanStatus.STARTED);
            verify(scheduleService).generateSchedule(entity);
            verify(loanRepository).save(entity);
        }

        @Test
        @DisplayName("if invalid personal code, then throws BusinessException INVALID_PERSONAL_CODE")
        void invalidPersonalCode_throwsException() {
            when(personalCodeValidator.isValid(VALID_CODE)).thenReturn(false);

            assertThatThrownBy(() -> loanService.submitApplication(validRequest))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_PERSONAL_CODE);

            verifyNoInteractions(loanRepository, scheduleService);
        }

        @Test
        @DisplayName("if existing active application, then throws BusinessException ACTIVE_APPLICATION_EXISTS")
        void existingActiveApplication_throwsException() {
            when(personalCodeValidator.isValid(VALID_CODE)).thenReturn(true);
            when(loanRepository.existsByPersonalCodeAndStatusIn(eq(VALID_CODE), anySet())).thenReturn(true);

            assertThatThrownBy(() -> loanService.submitApplication(validRequest))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.ACTIVE_APPLICATION_EXISTS);

            verifyNoInteractions(scheduleService);
        }

        @Test
        @DisplayName("if customer too old, then saves REJECTED with CUSTOMER_TOO_OLD reason")
        void customerTooOld_savesRejected() {
            when(personalCodeValidator.isValid(VALID_CODE)).thenReturn(true);
            when(loanRepository.existsByPersonalCodeAndStatusIn(eq(VALID_CODE), anySet())).thenReturn(false);
            when(personalCodeValidator.extractBirthDate(VALID_CODE))
                    .thenReturn(LocalDate.of(1940, 1, 1));
            when(configService.getConfig()).thenReturn(config);

            LoanApplication rejectedEntity = new LoanApplication();
            when(loanApplicationMapper.toEntity(validRequest)).thenReturn(rejectedEntity);
            when(loanRepository.save(rejectedEntity)).thenReturn(rejectedEntity);

            LoanApplicationResponse expectedResponse = LoanApplicationResponse.builder()
                    .status(LoanStatus.REJECTED)
                    .rejectionReason(RejectionReason.CUSTOMER_TOO_OLD)
                    .build();
            when(loanApplicationMapper.toDto(rejectedEntity)).thenReturn(expectedResponse);

            LoanApplicationResponse result = loanService.submitApplication(validRequest);

            assertThat(result.getStatus()).isEqualTo(LoanStatus.REJECTED);
            assertThat(result.getRejectionReason()).isEqualTo(RejectionReason.CUSTOMER_TOO_OLD);

            // Status should be set before save
            assertThat(rejectedEntity.getStatus()).isEqualTo(LoanStatus.REJECTED);
            assertThat(rejectedEntity.getRejectionReason()).isEqualTo(RejectionReason.CUSTOMER_TOO_OLD);

            verify(loanRepository).save(rejectedEntity);
            verifyNoInteractions(scheduleService);
        }

        @Test
        @DisplayName("if customer exactly at max age, then accepted (boundary)")
        void customerAtMaxAge_isAccepted() {
            when(personalCodeValidator.isValid(VALID_CODE)).thenReturn(true);
            when(loanRepository.existsByPersonalCodeAndStatusIn(eq(VALID_CODE), anySet()))
                    .thenReturn(false);
            // Age exactly 70 - should NOT be rejected
            LocalDate birthDateAtLimit = LocalDate.now().minusYears(70);
            when(personalCodeValidator.extractBirthDate(VALID_CODE)).thenReturn(birthDateAtLimit);
            when(configService.getConfig()).thenReturn(config);

            LoanApplication entity = LoanApplication.builder().status(LoanStatus.STARTED).build();
            when(loanApplicationMapper.toEntity(validRequest)).thenReturn(entity);
            when(loanRepository.save(entity)).thenReturn(entity);
            when(loanApplicationMapper.toDto(entity)).thenReturn(
                    LoanApplicationResponse.builder().status(LoanStatus.STARTED).build());

            LoanApplicationResponse result = loanService.submitApplication(validRequest);

            assertThat(result.getStatus()).isEqualTo(LoanStatus.STARTED);
            verify(scheduleService).generateSchedule(entity);
        }
    }


    @Nested
    @DisplayName("regenerateSchedule")
    class RegenerateSchedule {

        private ScheduleRegenerateRequest regenRequest;

        @BeforeEach
        void setUp() {
            regenRequest = new ScheduleRegenerateRequest();
            regenRequest.setInterestMargin(new BigDecimal("0.0200"));
            regenRequest.setLoanPeriodMonths(60);
        }

        @Test
        @DisplayName("if loan IN_REVIEW, then deletes old schedule, updates loan, generates new schedule")
        void inReviewLoan_regeneratesSchedule() {
            LoanApplication loan = LoanApplication.builder()
                    .id(1L)
                    .status(LoanStatus.IN_REVIEW)
                    .loanAmount(LOAN_AMOUNT)
                    .interestMargin(new BigDecimal("0.0200"))
                    .loanPeriodMonths(60)
                    .baseInterestRate(new BigDecimal("1.7000"))
                    .build();

            when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));
            when(configService.getConfig()).thenReturn(config);
            when(scheduleRepository.findByLoanApplicationIdOrderByPaymentNumberAsc(1L))
                    .thenReturn(Collections.emptyList());

            List<PaymentScheduleItem> result =
                    loanService.regenerateSchedule(1L, regenRequest);

            verify(scheduleRepository).deleteByLoanApplicationId(1L);
            verify(scheduleService).generateScheduleInternal(loan);
            assertThat(loan.getInterestMargin()).isEqualByComparingTo(new BigDecimal("0.0200"));
            assertThat(loan.getLoanPeriodMonths()).isEqualTo(60);
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("if loan not found, then throws BusinessException APPLICATION_NOT_FOUND")
        void loanNotFound_throwsException() {
            when(loanRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> loanService.regenerateSchedule(99L, regenRequest))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.APPLICATION_NOT_FOUND);
        }

        @Test
        @DisplayName("if loan APPROVED, then throws BusinessException APPLICATION_NOT_IN_REVIEW")
        void approvedLoan_throwsException() {
            LoanApplication loan = LoanApplication.builder()
                    .id(1L)
                    .status(LoanStatus.APPROVED)
                    .build();
            when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));

            assertThatThrownBy(() -> loanService.regenerateSchedule(1L, regenRequest))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.APPLICATION_NOT_IN_REVIEW);
        }

        @Test
        @DisplayName("if loan REJECTED, then throws BusinessException APPLICATION_NOT_IN_REVIEW")
        void rejectedLoan_throwsException() {
            LoanApplication loan = LoanApplication.builder()
                    .id(1L)
                    .status(LoanStatus.REJECTED)
                    .build();
            when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));

            assertThatThrownBy(() -> loanService.regenerateSchedule(1L, regenRequest))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.APPLICATION_NOT_IN_REVIEW);
        }

        @Test
        @DisplayName("if loan STARTED, then throws BusinessException APPLICATION_NOT_IN_REVIEW")
        void startedLoan_throwsException() {
            LoanApplication loan = LoanApplication.builder()
                    .id(1L)
                    .status(LoanStatus.STARTED)
                    .build();
            when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));

            assertThatThrownBy(() -> loanService.regenerateSchedule(1L, regenRequest))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.APPLICATION_NOT_IN_REVIEW);
        }
    }
}
