package com.timemate.gonow.domain.appointment.dto;

import com.timemate.gonow.domain.appointment.entity.Appointment;

public record AppointmentCreateResponse(
        Long appointmentId,
        String inviteCode
) {
    public static AppointmentCreateResponse from(Appointment appointment) {
        return new AppointmentCreateResponse(
                appointment.getId(),
                appointment.getInviteCode()
        );
    }
}
