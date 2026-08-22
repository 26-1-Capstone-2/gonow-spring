package com.timemate.gonow.domain.member.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record PasswordResetRequest(
        @NotBlank(message = "이메일 필수")
        @Email(message = "올바른 이메일 형식이 아님")
        String email,

        @NotBlank(message = "새 비밀번호 필수")
        @Pattern(regexp = "^[\\x21-\\x7E]{8,64}$", message = "비밀번호는 공백 없는 영문/숫자/특수문자로 8자 이상 64자 이하여야 합니다")
        String newPassword
) {}
