package com.timemate.gonow.global.client.dto;

import java.time.LocalDateTime;

public record FlaskJourneyResponse(
        LocalDateTime targetTime,        // 막차 모드에서만 값 있음, 데드라인 모드에서는 null
        LocalDateTime departureAlarmTime,
        Integer interval,
        String whichStation,             // 최초 탑승 지점 (지하철역/버스정류장), DRIVING이면 null
        LocalDateTime boardingTime       // 탑승 시각, DRIVING이면 null
) {}
