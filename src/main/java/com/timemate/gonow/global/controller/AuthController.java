package com.timemate.gonow.global.controller;

import com.timemate.gonow.global.dto.LoginRequest;
import com.timemate.gonow.global.dto.LoginResponse;
import com.timemate.gonow.global.response.ApiResult;
import com.timemate.gonow.global.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
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

        return ApiResult.success("로그인이 완료되었습니다", response);
    }

    // 미완성 -----------------------------------------------------------------------------------
    // 로그아웃
    // 현재는 AT만 사용하므로 실제 서버 처리 없음 — 클라이언트가 토큰을 삭제하는 것으로 로그아웃 처리.
    // 추후 RT 도입 시 이 메서드에 RT 무효화 로직을 추가하면 됨.
    @PostMapping("/logout")
    public ApiResult<Void> logout(@AuthenticationPrincipal UserDetails userDetails) {
        log.info("회원 로그아웃 - memberId: {}", userDetails.getUsername());

        // TODO: RT 만료 로직 추가 예정 (RT 도입 시)

        return ApiResult.success("로그아웃되었습니다");
    }
}
