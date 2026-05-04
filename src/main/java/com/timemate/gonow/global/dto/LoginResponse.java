package com.timemate.gonow.global.dto;

// 최초 로그인 시 프론트(앱)에 AT를 전달하는 용도
// 로그인 상태에서의 매 요청은 JWT 필터가 알아서 처리함
public record LoginResponse(
        Long id, // 우리 DB의 PK
        String jwtToken // 우리 서비스 전용 JWT
) {
    public static LoginResponse from(Long id, String jwtToken) {
        return new LoginResponse(id, jwtToken);
    }
}
