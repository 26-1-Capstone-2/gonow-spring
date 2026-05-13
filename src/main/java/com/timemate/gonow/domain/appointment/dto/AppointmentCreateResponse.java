package com.timemate.gonow.domain.appointment.dto;

import com.timemate.gonow.domain.appointment.constant.ParticipantStatus;
import com.timemate.gonow.domain.appointment.entity.Appointment;
import com.timemate.gonow.domain.appointment.entity.Participant;

import java.time.LocalDateTime;

public record AppointmentCreateResponse(
        Long appointmentId,
        String inviteCode,
        ParticipantStatus participantStatus,
        LocalDateTime departureAlarmTime
) {
    public static AppointmentCreateResponse from(Appointment appointment, Participant host) {
        return new AppointmentCreateResponse(
                appointment.getId(),
                appointment.getInviteCode(),
                host.getParticipantStatus(),
                host.getDepartureAlarmTime()
        );
    }
}
