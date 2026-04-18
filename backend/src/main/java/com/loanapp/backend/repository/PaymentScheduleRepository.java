package com.loanapp.backend.repository;


import com.loanapp.backend.domain.PaymentSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentScheduleRepository extends JpaRepository<PaymentSchedule, Long> {

    void deleteByLoanApplicationId(Long loanApplicationId);

    List<PaymentSchedule> findByLoanApplicationIdOrderByPaymentNumberAsc(Long loanApplicationId);
}
