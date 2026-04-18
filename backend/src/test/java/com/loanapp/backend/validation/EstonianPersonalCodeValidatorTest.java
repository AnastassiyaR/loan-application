package com.loanapp.backend.validation;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

class EstonianPersonalCodeValidatorTest {

    private EstonianPersonalCodeValidator validator;

    @BeforeEach
    void setUp() {
        validator = new EstonianPersonalCodeValidator();
    }


    @Nested
    @DisplayName("isValid()")
    class IsValid {

        @Test
        @DisplayName("valid male code born 1995 returns true")
        void validMaleCode_returnsTrue() {
            assertThat(validator.isValid("37605030299")).isTrue();
        }

        @Test
        @DisplayName("null input returns false")
        void nullInput_returnsFalse() {
            assertThat(validator.isValid(null)).isFalse();
        }

        @Test
        @DisplayName("empty string returns false")
        void emptyString_returnsFalse() {
            assertThat(validator.isValid("")).isFalse();
        }

        @Test
        @DisplayName("code shorter than 11 digits returns false")
        void tooShort_returnsFalse() {
            assertThat(validator.isValid("3760503029")).isFalse();
        }

        @Test
        @DisplayName("code longer than 11 digits returns false")
        void tooLong_returnsFalse() {
            assertThat(validator.isValid("376050302991")).isFalse();
        }

        @Test
        @DisplayName("code with non-digit characters returns false")
        void nonDigits_returnsFalse() {
            assertThat(validator.isValid("3760503029A")).isFalse();
        }

        @Test
        @DisplayName("wrong checksum returns false")
        void wrongChecksum_returnsFalse() {
            // Last digit changed from 9 to 0
            assertThat(validator.isValid("37605030290")).isFalse();
        }

        @Test
        @DisplayName("impossible date (month 13) returns false")
        void impossibleDate_returnsFalse() {
            // Month = 13 is invalid
            assertThat(validator.isValid("37613030000")).isFalse();
        }

        @Test
        @DisplayName("impossible date (day 32) returns false")
        void impossibleDay_returnsFalse() {
            assertThat(validator.isValid("37605320000")).isFalse();
        }
    }

    @Nested
    @DisplayName("extractBirthDate()")
    class ExtractBirthDate {

        @Test
        @DisplayName("if century marker 3, then born in 1900s")
        void centuryMarker3_1900s() {
            LocalDate date = validator.extractBirthDate("37605030000");
            assertThat(date).isEqualTo(LocalDate.of(1976, 5, 3));
        }

        @Test
        @DisplayName("if century marker 4, then born in 1900s (female)")
        void centuryMarker4_1900s() {
            LocalDate date = validator.extractBirthDate("49503160000");
            assertThat(date).isEqualTo(LocalDate.of(1995, 3, 16));
        }

        @Test
        @DisplayName("if century marker 5, then born in 2000s")
        void centuryMarker5_2000s() {
            LocalDate date = validator.extractBirthDate("50103160000");
            assertThat(date).isEqualTo(LocalDate.of(2001, 3, 16));
        }

        @Test
        @DisplayName("if century marker 6, then born in 2000s (female)")
        void centuryMarker6_2000s() {
            LocalDate date = validator.extractBirthDate("61005150000");
            assertThat(date).isEqualTo(LocalDate.of(2010, 5, 15));
        }

        @Test
        @DisplayName("invalid century marker throws IllegalArgumentException")
        void invalidCenturyMarker_throws() {
            assertThatThrownBy(() -> validator.extractBirthDate("07605030000"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid century marker");
        }

        @Test
        @DisplayName("impossible date throws exception")
        void impossibleDate_throws() {
            // day 32 is invalid
            assertThatThrownBy(() -> validator.extractBirthDate("37605320000"))
                    .isInstanceOf(Exception.class);
        }
    }
}
