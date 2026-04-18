package com.loanapp.backend.service;


import com.loanapp.backend.dto.AppConfigDto;
import com.loanapp.backend.exception.BusinessException;
import com.loanapp.backend.exception.ErrorCode;
import com.loanapp.backend.mapper.AppConfigMapper;
import com.loanapp.backend.domain.AppConfig;
import com.loanapp.backend.repository.AppConfigRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ConfigService {

    private final AppConfigRepository appConfigRepository;
    private final AppConfigMapper appConfigMapper;
    private static final Long CONFIG_ID = 1L;

    public AppConfigDto getConfig() {
        AppConfig appConfig = getAppConfig();
        return appConfigMapper.toDto(appConfig);
    }

    @Transactional
    public AppConfigDto updateConfig(AppConfigDto request) {
        AppConfig appConfig = getAppConfig();

        appConfig.setMaxCustomerAge(request.getMaxCustomerAge());
        appConfig.setBaseInterestRate(request.getBaseInterestRate());

        return appConfigMapper.toDto(appConfig);
    }

    public AppConfig getAppConfig() {
        return appConfigRepository.findById(CONFIG_ID)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONFIG_NOT_FOUND));
    }
}
