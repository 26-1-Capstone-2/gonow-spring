package com.timemate.gonow.global.client.dto;

import java.time.LocalDateTime;

public record FlaskParticipantResponse(
        LocalDateTime departureAlarmTime,
        LocalDateTime estimatedArrival,
        Integer interval,
        String whichStation,             // 최초 탑승 지점 (지하철역/버스정류장), DRIVING이면 null
        LocalDateTime boardingTime       // 탑승 시각, DRIVING이면 null
) {}
