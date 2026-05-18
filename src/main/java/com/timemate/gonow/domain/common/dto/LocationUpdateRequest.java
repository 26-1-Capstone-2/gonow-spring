package com.timemate.gonow.domain.common.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record LocationUpdateRequest(
        @NotNull BigDecimal lat,
        @NotNull BigDecimal lng
) {}
