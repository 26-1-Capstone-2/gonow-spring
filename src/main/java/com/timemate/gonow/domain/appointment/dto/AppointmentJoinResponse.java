package com.timemate.gonow.domain.appointment.dto;

import com.timemate.gonow.domain.appointment.entity.Appointment;

public record AppointmentJoinResponse(
        Long appointmentId
) {
    public static AppointmentJoinResponse from(Appointment appointment) {
        return new AppointmentJoinResponse(
                appointment.getId()
        );
    }
}
