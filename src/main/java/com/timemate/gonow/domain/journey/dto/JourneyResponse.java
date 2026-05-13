package com.timemate.gonow.domain.journey.dto;

import com.timemate.gonow.domain.journey.constant.JourneyStatus;
import com.timemate.gonow.domain.journey.entity.Journey;

import java.time.LocalDateTime;

public record JourneyResponse(
        Long journeyId,
        JourneyStatus journeyStatus,
        LocalDateTime departureAlarmTime
) {
    public static JourneyResponse from(Journey journey) {
        return new JourneyResponse(
                journey.getId(),
                journey.getJourneyStatus(),
                journey.getDepartureAlarmTime()
        );
    }
}
