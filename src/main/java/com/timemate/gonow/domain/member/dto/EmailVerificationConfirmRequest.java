package com.timemate.gonow.domain.member.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record EmailVerificationConfirmRequest(
        @NotBlank(message = "이메일 필수")
        @Email(message = "올바른 이메일 형식이 아님")
        String email,

        @NotBlank(message = "인증코드 필수")
        @Pattern(regexp = "^[0-9]{6}$", message = "인증코드는 6자리 숫자입니다")
        String code
) {}
