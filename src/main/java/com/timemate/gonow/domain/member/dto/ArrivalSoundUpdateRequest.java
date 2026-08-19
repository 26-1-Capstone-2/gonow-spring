package com.timemate.gonow.domain.member.dto;

import com.timemate.gonow.domain.member.constant.AlarmSoundMode;

// 도착 예정/완료 알림 소리 모드 부분 업데이트 — 프론트가 토글 하나 누를 때마다 즉시 저장
// -> 둘을 한 화면에서 같이 "저장" 버튼으로 제출하는 게 아님.
// 바뀐 필드만 보내도 되게 둘 다 nullable로 둔다.
public record ArrivalSoundUpdateRequest(
        AlarmSoundMode arrivalExpectedSoundMode,
        AlarmSoundMode arrivalCompleteSoundMode
) {}
