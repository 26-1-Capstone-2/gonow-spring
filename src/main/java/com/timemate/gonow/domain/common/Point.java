package com.timemate.gonow.domain.common;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.math.BigDecimal;

@Embeddable
public record Point(
        @Column(nullable = false, precision = 10, scale = 8)
        BigDecimal lat,

        @Column(nullable = false, precision = 11, scale = 8)
        BigDecimal lng
) {}
