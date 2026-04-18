package com.loanapp.backend.exception;


public class TechnicalException extends RuntimeException {
    public TechnicalException(String message) {
        super(message);
    }
}
