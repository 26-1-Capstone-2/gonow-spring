package com.timemate.gonow.domain.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record PasswordUpdateRequest(
        @NotBlank(message = "현재 비밀번호 필수")
        String currentPassword,

        @NotBlank(message = "새 비밀번호 필수")
        @Pattern(regexp = "^[\\x21-\\x7E]{8,64}$", message = "비밀번호는 공백 없는 영문/숫자/특수문자로 8자 이상 64자 이하여야 합니다")
        String newPassword
) {}