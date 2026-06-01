package com.timemate.gonow.domain.appointment.dto;

import com.timemate.gonow.domain.appointment.entity.Appointment;
import com.timemate.gonow.domain.appointment.entity.Participant;

public record AppointmentCreateResponse(
        Long appointmentId,
        String inviteCode,
        String participantStatus  // 방장의 초기 상태 (당일이면 READY, 그 외엔 SCHEDULED)
) {
    public static AppointmentCreateResponse from(Appointment appointment, Participant host) {
        return new AppointmentCreateResponse(
                appointment.getId(),
                appointment.getInviteCode(),
                host.getParticipantStatus().name()
        );
    }
}
