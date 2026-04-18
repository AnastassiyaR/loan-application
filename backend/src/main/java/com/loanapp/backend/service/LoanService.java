package com.loanapp.backend.service;


import com.loanapp.backend.dto.LoanApplicationRequest;
import com.loanapp.backend.dto.LoanApplicationResponse;
import com.loanapp.backend.dto.PaymentScheduleItem;
import com.loanapp.backend.dto.ScheduleRegenerateRequest;
import com.loanapp.backend.exception.BusinessException;
import com.loanapp.backend.exception.ErrorCode;
import com.loanapp.backend.mapper.LoanApplicationMapper;
import com.loanapp.backend.mapper.PaymentScheduleMapper;
import com.loanapp.backend.domain.LoanApplication;
import com.loanapp.backend.domain.LoanStatus;
import com.loanapp.backend.domain.RejectionReason;
import com.loanapp.backend.repository.LoanApplicationRepository;
import com.loanapp.backend.repository.PaymentScheduleRepository;
import com.loanapp.backend.validation.EstonianPersonalCodeValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LoanService {

    private final LoanApplicationRepository loanRepository;
    private final ScheduleService scheduleService;
    private final EstonianPersonalCodeValidator personalCodeValidator;
    private final ConfigService configService;
    private final LoanApplicationMapper loanApplicationMapper;
    private final PaymentScheduleRepository scheduleRepository;
    private final PaymentScheduleMapper paymentScheduleMapper;

    @Transactional
    public LoanApplicationResponse submitApplication(LoanApplicationRequest request) {
        // Validate personal code
        if (!personalCodeValidator.isValid(request.getPersonalCode())) {
            throw new BusinessException(ErrorCode.INVALID_PERSONAL_CODE);
        }

        // Check active applications
        boolean hasActive = loanRepository.existsByPersonalCodeAndStatusIn(
                request.getPersonalCode(),
                LoanStatus.activeStatuses()
        );
        if (hasActive) {
            throw new BusinessException(ErrorCode.ACTIVE_APPLICATION_EXISTS);
        }

        // Age check
        LocalDate birthDate = personalCodeValidator.extractBirthDate(request.getPersonalCode());
        int age = Period.between(birthDate, LocalDate.now()).getYears();
        if (age > configService.getConfig().getMaxCustomerAge()) {
            LoanApplication rejected = createLoanFromRequest(request);
            rejected.setStatus(LoanStatus.REJECTED);
            rejected.setRejectionReason(RejectionReason.CUSTOMER_TOO_OLD);
            LoanApplication saved = loanRepository.save(rejected);
            return loanApplicationMapper.toDto(saved);  // 201, status=REJECTED
        }

        LoanApplication loan = createLoanFromRequest(request);
        loan.setStatus(LoanStatus.STARTED);
        loan = loanRepository.save(loan);
        scheduleService.generateSchedule(loan);
        return loanApplicationMapper.toDto(loan);
    }

    public List<LoanApplicationResponse> getAll() {
        return loanRepository.findAll()
                .stream()
                .map(loan -> LoanApplicationResponse.builder()
                        .id(loan.getId())
                        .firstName(loan.getFirstName())
                        .lastName(loan.getLastName())
                        .personalCode(loan.getPersonalCode())
                        .loanAmount(loan.getLoanAmount())
                        .loanPeriodMonths(loan.getLoanPeriodMonths())
                        .status(loan.getStatus())
                        .createdAt(loan.getCreatedAt())
                        .build())
                .toList();
    }

    public LoanApplicationResponse getById(Long id) {
        LoanApplication loan = loanRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.APPLICATION_NOT_FOUND));
        return loanApplicationMapper.toDto(loan);
    }

    @Transactional
    public List<PaymentScheduleItem> regenerateSchedule(Long id, ScheduleRegenerateRequest request) {
        LoanApplication loan = loanRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.APPLICATION_NOT_FOUND));

        if (loan.getStatus() != LoanStatus.IN_REVIEW) {
            throw new BusinessException(ErrorCode.APPLICATION_NOT_IN_REVIEW);
        }

        scheduleRepository.deleteByLoanApplicationId(id);

        loan.setBaseInterestRate(configService.getConfig().getBaseInterestRate());
        loan.setInterestMargin(request.getInterestMargin());
        loan.setLoanPeriodMonths(request.getLoanPeriodMonths());

        scheduleService.generateScheduleInternal(loan);

        return scheduleRepository.findByLoanApplicationIdOrderByPaymentNumberAsc(id)
                .stream()
                .map(paymentScheduleMapper::toDto)
                .toList();
    }

    private LoanApplication createLoanFromRequest(LoanApplicationRequest request) {
        LoanApplication loan = loanApplicationMapper.toEntity(request);
        loan.setBaseInterestRate(configService.getConfig().getBaseInterestRate());
        return loan;
    }
}
