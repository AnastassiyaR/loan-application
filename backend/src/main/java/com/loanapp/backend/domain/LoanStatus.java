package com.loanapp.backend.domain;


import java.util.EnumSet;
import java.util.Set;

public enum LoanStatus {
    STARTED,
    IN_REVIEW,
    APPROVED,
    REJECTED;

    /**
     * Returns statuses considered "active" (still in progress) for business rules.
     */
    public static Set<LoanStatus> activeStatuses() {
        return EnumSet.of(STARTED, IN_REVIEW);
    }
}
