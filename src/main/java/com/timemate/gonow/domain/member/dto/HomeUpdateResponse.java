package com.timemate.gonow.domain.member.dto;

import com.timemate.gonow.domain.common.Location;

import java.math.BigDecimal;

public record HomeUpdateResponse(
        String address,
        BigDecimal lat,
        BigDecimal lng
) {
    // Location -> UpdateHomeResponse 전환
    public static HomeUpdateResponse from(Location location) {
        return new HomeUpdateResponse(
                location.address(),
                location.point().lat(),
                location.point().lng()
        );
    }
}
