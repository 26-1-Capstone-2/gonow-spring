package com.timemate.gonow.global.controller;

import com.timemate.gonow.global.response.ApiResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {
    @GetMapping("/health")
    public ApiResult<String> health() {
        return ApiResult.success("OK");
    }
}
