package com.loanapp.backend.exception;


public enum ErrorCode {

    // Validation errors
    INVALID_PERSONAL_CODE,
    INVALID_PARAMETER,

    // Business logic
    ACTIVE_APPLICATION_EXISTS,
    APPLICATION_NOT_FOUND,
    APPLICATION_NOT_IN_REVIEW,

    // Workflow
    INVALID_STATUS_TRANSITION,

    // Other
    CONFIG_NOT_FOUND,
    TECHNICAL_ERROR,
    UNEXPECTED_ERROR;
}
