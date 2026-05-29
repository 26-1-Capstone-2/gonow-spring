package com.timemate.gonow.global.client.dto;

import com.timemate.gonow.domain.member.constant.PriorityType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record FlaskParticipantRequest(
        Long memberId,
        BigDecimal currentLat,
        BigDecimal currentLng,
        BigDecimal destLat,
        BigDecimal destLng,
        TransportMode transportMode,
        PriorityType priorityType,
        LocalDateTime targetTime,
        int preparationTime
) {}
