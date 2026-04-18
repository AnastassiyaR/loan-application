package com.loanapp.backend.service;


import com.loanapp.backend.domain.LoanApplication;
import com.loanapp.backend.domain.LoanStatus;
import com.loanapp.backend.domain.RejectionReason;
import com.loanapp.backend.dto.LoanApplicationResponse;
import com.loanapp.backend.exception.BusinessException;
import com.loanapp.backend.exception.ErrorCode;
import com.loanapp.backend.repository.LoanApplicationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private LoanApplicationRepository loanRepository;

    @InjectMocks
    private ReviewService reviewService;


    @Nested
    @DisplayName("approve")
    class Approve {

        @Test
        @DisplayName("if IN_REVIEW loan, then sets status APPROVED and returns it")
        void inReviewLoan_approvesSuccessfully() {
            LoanApplication loan = LoanApplication.builder()
                    .id(1L)
                    .status(LoanStatus.IN_REVIEW)
                    .build();
            when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));

            LoanApplicationResponse result = reviewService.approve(1L);

            assertThat(result.getStatus()).isEqualTo(LoanStatus.APPROVED);
            assertThat(loan.getStatus()).isEqualTo(LoanStatus.APPROVED);
        }

        @Test
        @DisplayName("if non-existing loan, then throws BusinessException APPLICATION_NOT_FOUND")
        void loanNotFound_throwsException() {
            when(loanRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> reviewService.approve(99L))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.APPLICATION_NOT_FOUND);
        }

        @Test
        @DisplayName("if APPROVED loan, then throws BusinessException APPLICATION_NOT_IN_REVIEW")
        void alreadyApprovedLoan_throwsException() {
            LoanApplication loan = LoanApplication.builder()
                    .id(1L)
                    .status(LoanStatus.APPROVED)
                    .build();
            when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));

            assertThatThrownBy(() -> reviewService.approve(1L))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.APPLICATION_NOT_IN_REVIEW);
        }

        @Test
        @DisplayName("if REJECTED loan, then throws BusinessException APPLICATION_NOT_IN_REVIEW")
        void rejectedLoan_throwsException() {
            LoanApplication loan = LoanApplication.builder()
                    .id(1L)
                    .status(LoanStatus.REJECTED)
                    .build();
            when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));

            assertThatThrownBy(() -> reviewService.approve(1L))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.APPLICATION_NOT_IN_REVIEW);
        }

        @Test
        @DisplayName("if STARTED loan, then throws BusinessException APPLICATION_NOT_IN_REVIEW")
        void startedLoan_throwsException() {
            LoanApplication loan = LoanApplication.builder()
                    .id(1L)
                    .status(LoanStatus.STARTED)
                    .build();
            when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));

            assertThatThrownBy(() -> reviewService.approve(1L))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.APPLICATION_NOT_IN_REVIEW);
        }
    }


    @Nested
    @DisplayName("reject")
    class Reject {

        @Test
        @DisplayName("if IN_REVIEW loan, then sets status REJECTED with given reason")
        void inReviewLoan_rejectsWithReason() {
            LoanApplication loan = LoanApplication.builder()
                    .id(1L)
                    .status(LoanStatus.IN_REVIEW)
                    .build();
            when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));

            LoanApplicationResponse result = reviewService.reject(1L, RejectionReason.CUSTOMER_TOO_OLD);

            assertThat(result.getStatus()).isEqualTo(LoanStatus.REJECTED);
            assertThat(result.getRejectionReason()).isEqualTo(RejectionReason.CUSTOMER_TOO_OLD);
            assertThat(loan.getStatus()).isEqualTo(LoanStatus.REJECTED);
            assertThat(loan.getRejectionReason()).isEqualTo(RejectionReason.CUSTOMER_TOO_OLD);
        }

        @Test
        @DisplayName("if non-existing loan, then throws BusinessException APPLICATION_NOT_FOUND")
        void loanNotFound_throwsException() {
            when(loanRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> reviewService.reject(99L, RejectionReason.CUSTOMER_TOO_OLD))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.APPLICATION_NOT_FOUND);
        }

        @Test
        @DisplayName("if APPROVED loan, then throws BusinessException APPLICATION_NOT_IN_REVIEW")
        void approvedLoan_throwsException() {
            LoanApplication loan = LoanApplication.builder()
                    .id(1L)
                    .status(LoanStatus.APPROVED)
                    .build();
            when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));

            assertThatThrownBy(() -> reviewService.reject(1L, RejectionReason.CUSTOMER_TOO_OLD))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.APPLICATION_NOT_IN_REVIEW);
        }

        @Test
        @DisplayName("if REJECTED loan, then throws BusinessException APPLICATION_NOT_IN_REVIEW")
        void alreadyRejectedLoan_throwsException() {
            LoanApplication loan = LoanApplication.builder()
                    .id(1L)
                    .status(LoanStatus.REJECTED)
                    .build();
            when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));

            assertThatThrownBy(() -> reviewService.reject(1L, RejectionReason.CUSTOMER_TOO_OLD))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.APPLICATION_NOT_IN_REVIEW);
        }
    }
}
