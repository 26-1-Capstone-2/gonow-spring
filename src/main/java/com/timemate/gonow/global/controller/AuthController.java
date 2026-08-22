package com.timemate.gonow.global.controller;

import com.timemate.gonow.global.dto.LoginRequest;
import com.timemate.gonow.global.dto.LoginResponse;
import com.timemate.gonow.global.dto.ReissueRequest;
import com.timemate.gonow.global.dto.ReissueResponse;
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

    // Access Token 재발급 — Access Token 만료 시 재로그인 없이 Refresh Token으로 새 Access Token 발급
    // (Refresh Token도 매번 회전 — 응답의 refresh_token으로 클라이언트가 저장값을 갱신해야 함)
    @PostMapping("/reissue")
    public ApiResult<ReissueResponse> reissue(@Valid @RequestBody ReissueRequest request) {
        ReissueResponse response = authService.reissue(request);

        return ApiResult.success("토큰 재발급 완료", response);
    }

    // 로그아웃 — FCM 토큰 null 처리 (같은 기기로 다른 계정 로그인 시 알림 오발송 방지)
    @PostMapping("/logout")
    public ApiResult<Void> logout(@MemberId Long memberId) {
        authService.logout(memberId);
        return ApiResult.success("로그아웃 완료");
    }
}
