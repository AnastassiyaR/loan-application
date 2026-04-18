package com.loanapp.backend.validation;


import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class EstonianPersonalCodeValidator {

    /**
     * Validates Estonian personal code (isikukood).
     */
    public boolean isValid(String code) {
        if (code == null || code.length() != 11 || !code.matches("\\d{11}")) {
            return false;
        }

        // Validate date part
        try {
            extractBirthDate(code);
        } catch (Exception e) {
            return false;
        }

        // Validate checksum
        return validateChecksum(code);
    }

    /**
     * Extracts birth date from Estonian personal code.
     */
    public LocalDate extractBirthDate(String code) {
        int centuryMarker = Character.getNumericValue(code.charAt(0));
        int year = Integer.parseInt(code.substring(1, 3));
        int month = Integer.parseInt(code.substring(3, 5));
        int day = Integer.parseInt(code.substring(5, 7));

        int century;
        switch (centuryMarker) {
            case 1, 2 -> century = 1800;
            case 3, 4 -> century = 1900;
            case 5, 6 -> century = 2000;
            case 7, 8 -> century = 2100;
            default -> throw new IllegalArgumentException("Invalid century marker");
        }

        return LocalDate.of(century + year, month, day);
    }

    /**
     * Validates checksum using Estonian ID algorithm.
     */
    private boolean validateChecksum(String code) {
        int[] weights1 = {1, 2, 3, 4, 5, 6, 7, 8, 9, 1};
        int[] weights2 = {3, 4, 5, 6, 7, 8, 9, 1, 2, 3};

        int sum = 0;

        // First round
        for (int i = 0; i < 10; i++) {
            sum += Character.getNumericValue(code.charAt(i)) * weights1[i];
        }

        int remainder = sum % 11;
        if (remainder < 10) {
            return remainder == Character.getNumericValue(code.charAt(10));
        }

        // Second round
        sum = 0;
        for (int i = 0; i < 10; i++) {
            sum += Character.getNumericValue(code.charAt(i)) * weights2[i];
        }

        remainder = sum % 11;
        if (remainder < 10) {
            return remainder == Character.getNumericValue(code.charAt(10));
        }

        // If still 10, the checksum must be 0
        return Character.getNumericValue(code.charAt(10)) == 0;
    }
}
