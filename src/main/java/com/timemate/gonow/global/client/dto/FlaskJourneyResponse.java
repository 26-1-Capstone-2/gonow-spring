package com.timemate.gonow.global.client.dto;

import java.time.LocalDateTime;

public record FlaskJourneyResponse(
        LocalDateTime departureAlarmTime,
        Integer interval
) {}
