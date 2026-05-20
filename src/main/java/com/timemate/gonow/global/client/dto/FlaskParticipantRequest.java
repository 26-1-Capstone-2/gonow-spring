package com.timemate.gonow.global.client.dto;

import com.timemate.gonow.domain.common.constant.TransportType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record FlaskParticipantRequest(
        BigDecimal currentLat,
        BigDecimal currentLng,
        BigDecimal destLat,
        BigDecimal destLng,
        TransportType transportType,
        LocalDateTime targetTime,
        int preparationTime
) {}
