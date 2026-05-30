package com.timemate.gonow.global.client.dto;

import java.time.LocalDateTime;

public record FlaskJourneyResponse(
        LocalDateTime targetTime,        // 막차 모드에서만 값 있음, 데드라인 모드에서는 null
        LocalDateTime departureAlarmTime,
        Integer interval
) {}
