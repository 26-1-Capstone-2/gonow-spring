package com.timemate.gonow.domain.common;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.math.BigDecimal;

@Embeddable
public record Point(
        @Column(precision = 10, scale = 8)
        BigDecimal lat,     // 위도
        @Column(precision = 11, scale = 8)
        BigDecimal lng      // 경도
) {}
