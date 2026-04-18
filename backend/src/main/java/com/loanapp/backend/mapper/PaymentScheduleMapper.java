package com.loanapp.backend.mapper;


import com.loanapp.backend.dto.PaymentScheduleItem;
import com.loanapp.backend.domain.PaymentSchedule;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PaymentScheduleMapper {
    PaymentScheduleItem toDto(PaymentSchedule entity);
}
