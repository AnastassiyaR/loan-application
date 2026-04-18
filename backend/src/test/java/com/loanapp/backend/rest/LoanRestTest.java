package com.loanapp.backend.rest;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.loanapp.backend.domain.LoanStatus;
import com.loanapp.backend.repository.LoanApplicationRepository;
import com.loanapp.backend.repository.PaymentScheduleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class LoanRestTest extends BaseIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired LoanApplicationRepository loanRepository;
    @Autowired PaymentScheduleRepository scheduleRepository;

    private static final String CODE_1995 = "49503160001";
    private static final String CODE_1940 = "34001010001";
    private static final String CODE_1985 = "38506120007";

    @BeforeEach
    void cleanDb() {
        scheduleRepository.deleteAll();
        loanRepository.deleteAll();
    }

    private Map<String, Object> validRequest() {
        return Map.of(
                "firstName", "Anna",
                "lastName", "Ivanova",
                "personalCode", CODE_1995,
                "loanAmount", 15000.00,
                "loanPeriodMonths", 120,
                "interestMargin", 1.5,
                "baseInterestRate", 1.7
        );
    }

    private long submitAndGetId(Map<String, Object> req) throws Exception {
        var body = mockMvc.perform(post("/api/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(body).get("id").asLong();
    }


    @Nested
    @DisplayName("Loan Submission - POST /api/loans")
    class Submission {

        @Test
        @DisplayName("Valid request → 201 with id")
        void validRequest_returns201() throws Exception {
            mockMvc.perform(post("/api/loans")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest())))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").isNumber())
                    .andExpect(jsonPath("$.firstName").value("Anna"));

            var saved = loanRepository.findAll();
            assertThat(saved).hasSize(1);
            assertThat(saved.getFirst().getStatus()).isEqualTo(LoanStatus.IN_REVIEW);
        }

        @Test
        @DisplayName("Payment schedule generated")
        void scheduleGenerated() throws Exception {
            long id = submitAndGetId(validRequest());

            var schedule = scheduleRepository.findByLoanApplicationIdOrderByPaymentNumberAsc(id);
            assertThat(schedule).hasSize(120);
            assertThat(schedule.getFirst().getPaymentNumber()).isEqualTo(1);
        }

        @Test
        @DisplayName("If customer >70, then REJECTED")
        void tooOld_rejected() throws Exception {
            var req = Map.of(
                    "firstName", "Old",
                    "lastName", "Client",
                    "personalCode", CODE_1940,
                    "loanAmount", 10000.00,
                    "loanPeriodMonths", 60,
                    "interestMargin", 1.0,
                    "baseInterestRate", 1.7
            );

            mockMvc.perform(post("/api/loans")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status").value("REJECTED"))
                    .andExpect(jsonPath("$.rejectionReason").value("CUSTOMER_TOO_OLD"));
        }

        @Test
        @DisplayName("Only one active application per customer")
        void activeApplicationConflict() throws Exception {
            mockMvc.perform(post("/api/loans")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validRequest())));

            mockMvc.perform(post("/api/loans")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest())))
                    .andExpect(status().is4xxClientError())
                    .andExpect(jsonPath("$.errorCode").value("ACTIVE_APPLICATION_EXISTS"));
        }
    }

 
    @Nested
    @DisplayName("Loan Retrieval - GET /api/loans, GET /api/loans/{id}")
    class Retrieval {

        private void submit(String code) throws Exception {
            submitAndGetId(Map.of(
                    "firstName", "Test",
                    "lastName", "User",
                    "personalCode", code,
                    "loanAmount", 10000.00,
                    "loanPeriodMonths", 12,
                    "interestMargin", 1.5,
                    "baseInterestRate", 1.7
                    )
            );
        }

        @Nested
        @DisplayName("GET /api/loans")
        class GetAll {

            @Test
            @DisplayName("If empty DB, then empty list")
            void emptyDb() throws Exception {
                mockMvc.perform(get("/api/loans"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$", hasSize(0)));
            }

            @Test
            @DisplayName("If two applications, then list of two")
            void twoApps() throws Exception {
                submit(CODE_1995);
                submit(CODE_1985);

                mockMvc.perform(get("/api/loans"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$", hasSize(2)));
            }
        }

        @Nested
        @DisplayName("GET /api/loans/{id}")
        class GetById {

            @Test
            @DisplayName("Returns application with schedule")
            void returnsDetails() throws Exception {
                long id = submitAndGetId(Map.of(
                        "firstName", "Anna",
                        "lastName", "Test",
                        "personalCode", CODE_1995,
                        "loanAmount", 10000.00,
                        "loanPeriodMonths", 12,
                        "interestMargin", 1.5,
                        "baseInterestRate", 1.7
                        )
                );

                mockMvc.perform(get("/api/loans/{id}", id))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.id").value(id))
                        .andExpect(jsonPath("$.paymentSchedule", hasSize(12)));
            }

            @Test
            @DisplayName("If non-existent id, then APPLICATION_NOT_FOUND")
            void missingId() throws Exception {
                mockMvc.perform(get("/api/loans/{id}", 9999L))
                        .andExpect(status().is4xxClientError())
                        .andExpect(jsonPath("$.errorCode").value("APPLICATION_NOT_FOUND"));
            }
        }
    }
}
