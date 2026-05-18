package com.timemate.gonow.domain.journey.dto;

import com.timemate.gonow.domain.journey.constant.JourneyStatus;
import com.timemate.gonow.domain.journey.entity.Journey;

import java.time.LocalDateTime;

public record LocationUpdateResponse(
        JourneyStatus journeyStatus,
        LocalDateTime departureAlarmTime,
        LocalDateTime estimatedArrival
) {
    public static LocationUpdateResponse from(Journey journey) {
        return new LocationUpdateResponse(
                journey.getJourneyStatus(),
                journey.getDepartureAlarmTime(),
                journey.getEstimatedArrival()
        );
    }
}
