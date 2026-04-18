package com.loanapp.backend.service;


import com.loanapp.backend.dto.LoanApplicationResponse;
import com.loanapp.backend.exception.BusinessException;
import com.loanapp.backend.exception.ErrorCode;
import com.loanapp.backend.domain.LoanApplication;
import com.loanapp.backend.domain.LoanStatus;
import com.loanapp.backend.domain.RejectionReason;
import com.loanapp.backend.repository.LoanApplicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final LoanApplicationRepository loanRepository;

    @Transactional
    public LoanApplicationResponse approve(Long id) {
        LoanApplication loan = loanRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.APPLICATION_NOT_FOUND));

        ensureInReview(loan);

        loan.setStatus(LoanStatus.APPROVED);

        return LoanApplicationResponse.builder()
                .status(loan.getStatus())
                .build();
    }

    @Transactional
    public LoanApplicationResponse reject(Long id, RejectionReason reason) {
        LoanApplication loan = loanRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.APPLICATION_NOT_FOUND));

        ensureInReview(loan);

        loan.setStatus(LoanStatus.REJECTED);
        loan.setRejectionReason(reason);

        return LoanApplicationResponse.builder()
                .status(loan.getStatus())
                .rejectionReason(loan.getRejectionReason())
                .build();
    }

    private void ensureInReview(LoanApplication loan) {
        if (loan.getStatus() != LoanStatus.IN_REVIEW) {
            throw new BusinessException(ErrorCode.APPLICATION_NOT_IN_REVIEW);
        }
    }
}
