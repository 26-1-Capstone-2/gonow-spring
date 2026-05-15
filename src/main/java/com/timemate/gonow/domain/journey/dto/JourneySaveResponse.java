package com.timemate.gonow.domain.journey.dto;

import com.timemate.gonow.domain.journey.constant.JourneyStatus;
import com.timemate.gonow.domain.journey.entity.Journey;

public record JourneySaveResponse(
        Long journeyId,
        JourneyStatus journeyStatus
) {
    public static JourneySaveResponse from(Journey journey) {
        return new JourneySaveResponse(
                journey.getId(),
                journey.getJourneyStatus()
        );
    }
}
