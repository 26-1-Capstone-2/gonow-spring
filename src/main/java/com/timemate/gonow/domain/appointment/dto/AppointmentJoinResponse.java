package com.timemate.gonow.domain.appointment.dto;

import com.timemate.gonow.domain.appointment.entity.Appointment;
import com.timemate.gonow.domain.appointment.entity.Participant;

public record AppointmentJoinResponse(
        Long appointmentId,
        String participantStatus  // 참가자의 초기 상태 (당일이면 READY, 그 외엔 SCHEDULED)
) {
    public static AppointmentJoinResponse from(Appointment appointment, Participant participant) {
        return new AppointmentJoinResponse(
                appointment.getId(),
                participant.getParticipantStatus().name()
        );
    }
}
