package com.timemate.gonow.global.client.dto;

import com.timemate.gonow.domain.common.constant.TransportType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record FlaskJourneyRequest(
        BigDecimal currentLat,
        BigDecimal currentLng,
        BigDecimal destLat,
        BigDecimal destLng,
        TransportType transportType,
        LocalDateTime targetTime,
        boolean isLastMode,
        int preparationTime
) {}
