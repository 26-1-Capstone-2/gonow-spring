package com.timemate.gonow.domain.common;

import jakarta.persistence.Embeddable;

@Embeddable
public record Location(
        String address, // 주소
        Point point     // (위도, 경도)
) {}
