package com.timemate.gonow.global.dto;

// 최초 로그인 시 프론트(앱)에 AT/RT를 전달하는 용도
// 로그인 상태에서의 매 요청은 JWT 필터가 알아서 처리함
public record LoginResponse(
        Long memberId, // 우리 DB의 PK
        String accessToken, // 우리 서비스 전용 JWT — 유효기간 짧음(jwt.expiration), 만료되면 아래 refreshToken으로 재발급
        String refreshToken // Access Token 재발급 전용 — 유효기간 김(jwt.refresh-expiration), Redis에 회원별로 저장됨
) {
    public static LoginResponse from(Long id, String accessToken, String refreshToken) {
        return new LoginResponse(id, accessToken, refreshToken);
    }
}
