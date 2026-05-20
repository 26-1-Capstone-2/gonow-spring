package com.timemate.gonow.domain.journey.dto;

import com.timemate.gonow.domain.common.constant.TransportType;
import com.timemate.gonow.domain.journey.constant.JourneyType;
import com.timemate.gonow.domain.journey.entity.Journey;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record JourneyResponse(
        Long journeyId,
        JourneyType journeyType,
        boolean isLastMode,
        LocalDate planDate,
        LocalDateTime targetTime,
        String destName,
        String destAddress,
        BigDecimal destLat,
        BigDecimal destLng,
        TransportType transportType,
        int repeatDays,
        boolean isActive
) {
    public static JourneyResponse from(Journey journey) {
        return new JourneyResponse(
                journey.getId(),
                journey.getJourneyType(),
                journey.isLastMode(),
                journey.getPlanDate(),
                journey.getTargetTime(),
                journey.getDestination().name(),
                journey.getDestination().address(),
                journey.getDestination().point().lat(),
                journey.getDestination().point().lng(),
                journey.getTransportType(),
                journey.getRepeatDays(),
                journey.isActive()
        );
    }
}
