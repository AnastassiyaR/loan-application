package com.loanapp.backend.rest;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.loanapp.backend.domain.LoanStatus;
import com.loanapp.backend.domain.RejectionReason;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ReviewRestTest extends BaseIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired LoanApplicationRepository loanRepository;
    @Autowired PaymentScheduleRepository scheduleRepository;

    private static final String VALID_CODE = "37605030299";

    @BeforeEach
    void cleanDb() {
        scheduleRepository.deleteAll();
        loanRepository.deleteAll();
    }

    private long submitAndGetId() throws Exception {
        var request = Map.of(
                "firstName", "A",
                "lastName", "B",
                "personalCode", VALID_CODE,
                "loanAmount", 10000.00,
                "loanPeriodMonths", 24,
                "interestMargin", 1.5,
                "baseInterestRate", 1.0
        );

        var body = mockMvc.perform(post("/api/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(body).get("id").asLong();
    }


    @Nested
    @DisplayName("Approve - POST /api/loans/{id}/review/approve")
    class Approve {

        @Test
        @DisplayName("If IN_REVIEW application, then 200 with status APPROVED")
        void inReview_approvesSuccessfully() throws Exception {
            long id = submitAndGetId();

            mockMvc.perform(post("/api/loans/{id}/review/approve", id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("APPROVED"));

            var loan = loanRepository.findById(id).orElseThrow();
            assertThat(loan.getStatus()).isEqualTo(LoanStatus.APPROVED);
        }

        @Test
        @DisplayName("If non-existent id, then 4xx with APPLICATION_NOT_FOUND")
        void missingId_returnsNotFound() throws Exception {
            mockMvc.perform(post("/api/loans/{id}/review/approve", 9999L))
                    .andExpect(status().is4xxClientError())
                    .andExpect(jsonPath("$.errorCode").value("APPLICATION_NOT_FOUND"));
        }

        @Test
        @DisplayName("If already APPROVED application, then 4xx with APPLICATION_NOT_IN_REVIEW")
        void alreadyApproved_returnsError() throws Exception {
            long id = submitAndGetId();

            // First approve
            mockMvc.perform(post("/api/loans/{id}/review/approve", id))
                    .andExpect(status().isOk());

            // Try to approve again
            mockMvc.perform(post("/api/loans/{id}/review/approve", id))
                    .andExpect(status().is4xxClientError())
                    .andExpect(jsonPath("$.errorCode").value("APPLICATION_NOT_IN_REVIEW"));
        }
    }


    @Nested
    @DisplayName("Reject - POST /api/loans/{id}/review/reject")
    class Reject {

        @Test
        @DisplayName("If IN_REVIEW application, then 200 with status REJECTED and provided reason")
        void inReview_rejectsSuccessfully() throws Exception {
            long id = submitAndGetId();

            mockMvc.perform(post("/api/loans/{id}/review/reject", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("\"CUSTOMER_TOO_OLD\""))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("REJECTED"))
                    .andExpect(jsonPath("$.rejectionReason").value("CUSTOMER_TOO_OLD"));

            var loan = loanRepository.findById(id).orElseThrow();
            assertThat(loan.getStatus()).isEqualTo(LoanStatus.REJECTED);
            assertThat(loan.getRejectionReason()).isEqualTo(RejectionReason.CUSTOMER_TOO_OLD);
        }


        @Test
        @DisplayName("If non-existent id, then 4xx with APPLICATION_NOT_FOUND")
        void missingId_returnsNotFound() throws Exception {
            mockMvc.perform(post("/api/loans/{id}/review/reject", 9999L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("\"CUSTOMER_TOO_OLD\""))
                    .andExpect(status().is4xxClientError())
                    .andExpect(jsonPath("$.errorCode").value("APPLICATION_NOT_FOUND"));
        }

        @Test
        @DisplayName("If already REJECTED application, then 4xx with APPLICATION_NOT_IN_REVIEW")
        void alreadyRejected_returnsError() throws Exception {
            long id = submitAndGetId();

            mockMvc.perform(post("/api/loans/{id}/review/reject", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("\"CUSTOMER_TOO_OLD\""))
                    .andExpect(status().isOk());

            // Try to reject again
            mockMvc.perform(post("/api/loans/{id}/review/reject", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("\"CUSTOMER_TOO_OLD\""))
                    .andExpect(status().is4xxClientError())
                    .andExpect(jsonPath("$.errorCode").value("APPLICATION_NOT_IN_REVIEW"));
        }

        @Test
        @DisplayName("APPROVED application cannot be rejected")
        void approvedLoan_cannotBeRejected() throws Exception {
            long id = submitAndGetId();

            mockMvc.perform(post("/api/loans/{id}/review/approve", id));

            mockMvc.perform(post("/api/loans/{id}/review/reject", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("\"CUSTOMER_TOO_OLD\""))
                    .andExpect(status().is4xxClientError())
                    .andExpect(jsonPath("$.errorCode").value("APPLICATION_NOT_IN_REVIEW"));
        }
    }
}
