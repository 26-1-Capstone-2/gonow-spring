package com.timemate.gonow.global.controller;

import com.timemate.gonow.global.dto.LoginRequest;
import com.timemate.gonow.global.dto.LoginResponse;
import com.timemate.gonow.global.response.ApiResult;
import com.timemate.gonow.global.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.timemate.gonow.global.auth.MemberId;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    // 로그인
    @PostMapping("/login")
    public ApiResult<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);

        return ApiResult.success("로그인 완료", response);
    }

    // 로그아웃 — FCM 토큰 null 처리 (같은 기기로 다른 계정 로그인 시 알림 오발송 방지)
    @PostMapping("/logout")
    public ApiResult<Void> logout(@MemberId Long memberId) {
        authService.logout(memberId);
        return ApiResult.success("로그아웃 완료");
    }
}
