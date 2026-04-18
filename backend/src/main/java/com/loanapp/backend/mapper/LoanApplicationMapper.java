package com.loanapp.backend.mapper;


import com.loanapp.backend.dto.LoanApplicationRequest;
import com.loanapp.backend.dto.LoanApplicationResponse;
import com.loanapp.backend.domain.LoanApplication;
import org.mapstruct.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface LoanApplicationMapper {

   LoanApplication toEntity(LoanApplicationRequest request);

   LoanApplicationResponse toDto(LoanApplication loan);
}
