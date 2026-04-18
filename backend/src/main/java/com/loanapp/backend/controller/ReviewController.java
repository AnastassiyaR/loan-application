package com.loanapp.backend.controller;


import com.loanapp.backend.dto.LoanApplicationResponse;
import com.loanapp.backend.domain.RejectionReason;
import com.loanapp.backend.service.ReviewService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/loans/{id}/review")
@RequiredArgsConstructor
@Tag(name = "Loan Review", description = "Approve or reject loan applications")
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping("/approve")
    public LoanApplicationResponse approve(@PathVariable Long id) {
        return reviewService.approve(id);
    }

    @PostMapping("/reject")
    public LoanApplicationResponse reject(@PathVariable Long id, @RequestBody RejectionReason reason) {
        return reviewService.reject(id, reason);
    }
}
