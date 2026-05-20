package com.timemate.gonow.domain.appointment.dto;

import com.timemate.gonow.domain.appointment.constant.AppointmentStatus;
import com.timemate.gonow.domain.appointment.constant.ParticipantStatus;
import com.timemate.gonow.domain.appointment.entity.Appointment;
import com.timemate.gonow.domain.appointment.entity.Participant;

import java.time.LocalDateTime;

public record ParticipantLocationUpdateResponse(
        ParticipantStatus participantStatus,
        AppointmentStatus appointmentStatus,
        LocalDateTime departureAlarmTime,
        LocalDateTime estimatedArrival
) {
    public static ParticipantLocationUpdateResponse from(Participant participant, Appointment appointment) {
        return new ParticipantLocationUpdateResponse(
                participant.getParticipantStatus(),
                appointment.getAppointmentStatus(),
                participant.getDepartureAlarmTime(),
                participant.getEstimatedArrival()
        );
    }
}
