package com.timemate.gonow.global.dto;

// 프론트(앱)과 주고 받는 AT
public record LoginResponse(
        Long id, // 우리 DB의 PK
        String jwtToken // 우리 서비스 전용 JWT
) {}
