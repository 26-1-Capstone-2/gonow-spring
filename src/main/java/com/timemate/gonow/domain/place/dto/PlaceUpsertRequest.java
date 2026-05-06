package com.timemate.gonow.domain.place.dto;

import com.timemate.gonow.domain.place.constant.PlaceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PlaceUpsertRequest(
        String name,

        @NotNull(message = "장소 타입 필수")
        PlaceType placeType,

        @NotBlank(message = "주소 필수")
        String address,

        @NotNull(message = "위도 필수")
        BigDecimal lat,

        @NotNull(message = "경도 필수")
        BigDecimal lng
) {}
