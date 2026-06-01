package com.timemate.gonow.domain.appointment.dto;

import com.timemate.gonow.domain.appointment.constant.ParticipantStatus;

public record AppointmentUpdateResponse(
        String participantStatus  // 방장의 갱신된 상태 (날짜 변경 시 READY/SCHEDULED로 재조정)
) {
    public static AppointmentUpdateResponse from(ParticipantStatus status) {
        return new AppointmentUpdateResponse(status.name());
    }
}
