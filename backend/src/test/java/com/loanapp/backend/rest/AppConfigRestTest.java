package com.loanapp.backend.rest;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.loanapp.backend.domain.AppConfig;
import com.loanapp.backend.repository.AppConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AppConfigRestTest extends BaseIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired
    AppConfigRepository appConfigRepository;

    @BeforeEach
    void resetConfig() {
        appConfigRepository.deleteAll();

        AppConfig cfg =  AppConfig.builder()
                .id(1L)
                .maxCustomerAge(70)
                .baseInterestRate(new BigDecimal("3.847"))
                .build();

        appConfigRepository.save(cfg);
    }


    @Test
    @DisplayName("GET /api/config - params matches Liquibase")
    void getConfig_defaultMaxAge_is70() throws Exception {
        mockMvc.perform(get("/api/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maxCustomerAge").isNumber())
                .andExpect(jsonPath("$.maxCustomerAge").value(70))
                .andExpect(jsonPath("$.baseInterestRate").isNumber())
                .andExpect(jsonPath("$.baseInterestRate").value(3.847));
    }

    @Test
    @DisplayName("PUT /api/config - updates maxCustomerAge successfully")
    void putConfig_updatesMaxAge() throws Exception {
        var body = Map.of("maxCustomerAge", 65, "baseInterestRate", 3.847);

        mockMvc.perform(put("/api/config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maxCustomerAge").isNumber())
                .andExpect(jsonPath("$.maxCustomerAge").value(65))
                .andExpect(jsonPath("$.baseInterestRate").isNumber())
                .andExpect(jsonPath("$.baseInterestRate").value(3.847));
    }

    @Test
    @DisplayName("PUT /api/config - updates baseInterestRate successfully")
    void putConfig_updatesBaseInterestRate() throws Exception {
        var body = Map.of("maxCustomerAge", 70, "baseInterestRate", 4.847);

        mockMvc.perform(put("/api/config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maxCustomerAge").isNumber())
                .andExpect(jsonPath("$.maxCustomerAge").value(70))
                .andExpect(jsonPath("$.baseInterestRate").isNumber())
                .andExpect(jsonPath("$.baseInterestRate").value(4.847));
    }
}
