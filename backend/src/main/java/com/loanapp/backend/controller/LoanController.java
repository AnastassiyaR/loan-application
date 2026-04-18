package com.loanapp.backend.controller;


import com.loanapp.backend.dto.*;
import com.loanapp.backend.service.LoanService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/loans")
@RequiredArgsConstructor
@Tag(name = "Loan Applications", description = "Loan submission, review and schedule operations")
public class LoanController {

    private final LoanService loanService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LoanApplicationResponse submit(@Valid @RequestBody LoanApplicationRequest request) {
        return loanService.submitApplication(request);
    }

    @GetMapping
    public List<LoanApplicationResponse> getAll() {
        return loanService.getAll();
    }

    @GetMapping("/{id}")
    public LoanApplicationResponse getById(@PathVariable Long id) {
        return loanService.getById(id);
    }

    @PostMapping("/{id}/schedule/regenerate")
    public List<PaymentScheduleItem> regenerateSchedule(@PathVariable Long id,
                                                        @Valid @RequestBody ScheduleRegenerateRequest request) {
        return loanService.regenerateSchedule(id, request);
    }
}
