package com.timemate.gonow.global.client.dto;

import java.time.LocalDateTime;

public record FlaskParticipantResponse(
        LocalDateTime departureAlarmTime,
        LocalDateTime estimatedArrival,
        Integer interval
) {}
