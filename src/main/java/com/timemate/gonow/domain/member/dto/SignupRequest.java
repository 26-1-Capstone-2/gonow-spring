package com.timemate.gonow.domain.member.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

// 일반 JWT 회원가입
public record SignupRequest(
        @NotBlank(message = "이메일 필수")
        @Email(message = "올바른 이메일 형식이 아님")
        String email,

        @NotBlank(message = "비밀번호 필수")
        String password,

        @NotBlank(message = "닉네임 필수")
        String nickname
) {}
