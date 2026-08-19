package com.timemate.gonow.domain.appointment.constant;

import com.timemate.gonow.domain.member.constant.AlarmSoundMode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

// 도착 예정/완료 FCM 알림의 안드로이드 알림 채널. channelId 문자열은 프론트
// src/utils/notifications.ts의 ARRIVAL_EXPECTED_CHANNEL_IDS/ARRIVAL_COMPLETE_CHANNEL_IDS와
// 반드시 일치해야 함 — 회원별 소리/진동/무음 선호도(MemberSetting.arrivalExpectedSoundMode 등)에 따라
// 채널ID가 3가지로 갈리므로, base 문자열 뒤에 모드를 소문자로 이어붙여서 조합함(예: "gonow-arrival-expected-vibrate").
@Getter
@RequiredArgsConstructor
public enum ArrivalChannel {
    EXPECTED("gonow-arrival-expected"),
    COMPLETE("gonow-arrival-complete");

    private final String baseChannelId;

    public String getChannelId(AlarmSoundMode mode) {
        return baseChannelId + "-" + mode.name().toLowerCase();
    }
}
