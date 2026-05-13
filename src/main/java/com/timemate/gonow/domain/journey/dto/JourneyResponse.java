package com.timemate.gonow.domain.journey.dto;

import com.timemate.gonow.domain.journey.constant.JourneyStatus;
import com.timemate.gonow.domain.journey.entity.Journey;

public record JourneyResponse(
        Long journeyId,
        JourneyStatus journeyStatus
) {
    public static JourneyResponse from(Journey journey) {
        return new JourneyResponse(
                journey.getId(),
                journey.getJourneyStatus()
        );
    }
}
