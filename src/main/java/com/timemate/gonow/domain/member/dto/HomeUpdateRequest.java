package com.timemate.gonow.domain.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record HomeUpdateRequest(
        @NotBlank(message = "장소 이름 필수")
        String name,

        @NotBlank(message = "주소 필수")
        String address,

        @NotNull(message = "위도 필수")
        BigDecimal lat,

        @NotNull(message = "경도 필수")
        BigDecimal lng
) {}
