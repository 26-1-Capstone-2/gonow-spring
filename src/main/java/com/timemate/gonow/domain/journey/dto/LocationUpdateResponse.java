package com.timemate.gonow.domain.journey.dto;

import com.timemate.gonow.domain.journey.constant.JourneyStatus;
import com.timemate.gonow.domain.journey.entity.Journey;

import java.time.LocalDateTime;

public record LocationUpdateResponse(
        JourneyStatus journeyStatus,
        LocalDateTime departureAlarmTime,
        Integer interval,
        int preparationTime
) {
    public static LocationUpdateResponse from(Journey journey, Integer interval, int preparationTime) {
        return new LocationUpdateResponse(
                journey.getJourneyStatus(),
                journey.getDepartureAlarmTime(),
                interval,
                preparationTime
        );
    }
}
