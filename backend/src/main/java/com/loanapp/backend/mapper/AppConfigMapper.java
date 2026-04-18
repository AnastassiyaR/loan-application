package com.loanapp.backend.mapper;


import com.loanapp.backend.domain.AppConfig;
import com.loanapp.backend.dto.AppConfigDto;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AppConfigMapper {
    AppConfigDto toDto(AppConfig appConfig);
}
