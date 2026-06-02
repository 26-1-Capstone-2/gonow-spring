package com.timemate.gonow.domain.journey.dto;

import com.timemate.gonow.domain.journey.constant.JourneyStatus;
import com.timemate.gonow.domain.journey.entity.Journey;
import com.timemate.gonow.global.client.dto.FlaskJourneyResponse;

import java.time.LocalDateTime;

public record LocationUpdateResponse(
        JourneyStatus journeyStatus,
        LocalDateTime departureAlarmTime,
        Integer interval,
        int preparationTime,
        String whichStation,
        LocalDateTime boardingTime
) {
    public static LocationUpdateResponse from(Journey journey, Integer interval, int preparationTime, FlaskJourneyResponse flaskResponse) {
        return new LocationUpdateResponse(
                journey.getJourneyStatus(),
                journey.getDepartureAlarmTime(),
                interval,
                preparationTime,
                flaskResponse != null ? flaskResponse.whichStation() : null,
                flaskResponse != null ? flaskResponse.boardingTime() : null
        );
    }
}
