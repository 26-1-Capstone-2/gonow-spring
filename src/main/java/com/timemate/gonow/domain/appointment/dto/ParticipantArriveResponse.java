package com.timemate.gonow.domain.appointment.dto;

import com.timemate.gonow.domain.appointment.constant.AppointmentStatus;
import com.timemate.gonow.domain.appointment.entity.Appointment;

public record ParticipantArriveResponse(
        AppointmentStatus appointmentStatus
) {
    public static ParticipantArriveResponse from(Appointment appointment) {
        return new ParticipantArriveResponse(appointment.getAppointmentStatus());
    }
}
