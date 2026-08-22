package com.timemate.gonow.domain.member.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record EmailVerificationSendRequest(
        @NotBlank(message = "이메일 필수")
        @Email(message = "올바른 이메일 형식이 아님")
        String email
) {}
