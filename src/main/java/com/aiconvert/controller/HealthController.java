package com.aiconvert.controller;

import com.aiconvert.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class HealthController {

    @GetMapping(value = "/health", produces = "application/json;charset=UTF-8")
    public ApiResponse<String> health() {
        return ApiResponse.success("服务正常运行中");
    }
} 