package com.timemate.gonow.domain.member.dto;

import jakarta.validation.constraints.NotBlank;

public record NicknameUpdateRequest(
        @NotBlank(message = "닉네임은 필수입니다.")
        String nickname
) {}
