package com.timemate.gonow.global.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ReissueRequest(
        @NotNull(message = "회원 ID 필수")
        Long memberId,

        @NotBlank(message = "Refresh Token 필수")
        String refreshToken
) {}
