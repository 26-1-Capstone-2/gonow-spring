package com.timemate.gonow.domain.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record HomeUpdateRequest(
        @NotBlank(message = "주소는 필수입니다.")
        String address,

        @NotNull(message = "위도는 필수입니다.")
        BigDecimal lat,

        @NotNull(message = "경도는 필수입니다.")
        BigDecimal lng
) {}
