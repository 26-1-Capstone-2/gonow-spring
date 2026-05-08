package com.timemate.gonow.domain.member.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

// 일반 JWT 회원가입
public record SignupRequest(
        @NotBlank(message = "이메일 필수")
        @Email(message = "올바른 이메일 형식이 아님")
        String email,

        @NotBlank(message = "비밀번호 필수")
        String password,

        @NotBlank(message = "닉네임 필수")
        String nickname,

        @NotBlank(message = "집 이름 필수")
        String homeName,

        @NotBlank(message = "집 주소 필수")
        String homeAddress,

        @NotNull(message = "집 위도 필수")
        BigDecimal homeLat,

        @NotNull(message = "집 경도 필수")
        BigDecimal homeLng,

        @NotNull(message = "여유시간 필수")
        Integer preparationTime
) {}
