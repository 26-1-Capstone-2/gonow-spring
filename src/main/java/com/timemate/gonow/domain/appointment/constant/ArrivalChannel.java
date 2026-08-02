package com.timemate.gonow.domain.appointment.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

// 도착 예정/완료 FCM 알림의 안드로이드 알림 채널. channelId 문자열은 프론트
// src/utils/notifications.ts의 CHANNEL_BASE['arrival-expected']/CHANNEL_BASE['arrival-complete']와 반드시 일치해야 함.
@Getter
@RequiredArgsConstructor
public enum ArrivalChannel {
    EXPECTED("gonow-arrival-expected"),
    COMPLETE("gonow-arrival-complete");

    private final String channelId;
}
