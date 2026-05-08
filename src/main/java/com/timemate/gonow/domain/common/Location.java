package com.timemate.gonow.domain.common;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;

@Embeddable
public record Location(
        @Column(nullable = false)
        String name,

        @Column(nullable = false)
        String address,

        @Embedded
        Point point
) {}
