package com.timemate.gonow.domain.member.dto;

import jakarta.validation.constraints.NotBlank;

public record PasswordUpdateRequest(
        @NotBlank(message = "현재 비밀번호 필수")
        String currentPassword,

        @NotBlank(message = "새 비밀번호 필수")
        String newPassword
) {}