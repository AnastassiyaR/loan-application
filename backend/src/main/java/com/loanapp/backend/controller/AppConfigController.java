package com.loanapp.backend.controller;


import com.loanapp.backend.dto.AppConfigDto;
import com.loanapp.backend.service.ConfigService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/config")
@RequiredArgsConstructor
@Tag(name = "Application Configuration", description = "Manage dynamic application parameters")
public class AppConfigController {

    private final ConfigService configService;

    @GetMapping
    public AppConfigDto getConfig() {
        return configService.getConfig();
    }

    @PutMapping
    public AppConfigDto updateConfig(@Valid @RequestBody AppConfigDto request) {
        return configService.updateConfig(request);
    }
}
