package com.loanapp.backend.repository;


import com.loanapp.backend.domain.LoanApplication;
import com.loanapp.backend.domain.LoanStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;

@Repository
public interface LoanApplicationRepository extends JpaRepository<LoanApplication, Long> {
    boolean existsByPersonalCodeAndStatusIn(String personalCode, Collection<LoanStatus> status);
}
