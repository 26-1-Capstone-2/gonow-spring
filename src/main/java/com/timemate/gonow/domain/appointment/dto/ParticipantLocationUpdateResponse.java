package com.timemate.gonow.domain.appointment.dto;

import com.timemate.gonow.domain.appointment.constant.AppointmentStatus;
import com.timemate.gonow.domain.appointment.constant.ParticipantStatus;
import com.timemate.gonow.domain.appointment.entity.Appointment;
import com.timemate.gonow.domain.appointment.entity.Participant;
import com.timemate.gonow.global.client.dto.FlaskParticipantResponse;

import java.time.LocalDateTime;

public record ParticipantLocationUpdateResponse(
        ParticipantStatus participantStatus,
        AppointmentStatus appointmentStatus,
        LocalDateTime departureAlarmTime,
        LocalDateTime estimatedArrival,
        Integer interval,
        int preparationTime,
        String whichStation,
        LocalDateTime boardingTime
) {
    public static ParticipantLocationUpdateResponse from(Participant participant, Appointment appointment, Integer interval, int preparationTime, FlaskParticipantResponse flaskResponse) {
        return new ParticipantLocationUpdateResponse(
                participant.getParticipantStatus(),
                appointment.getAppointmentStatus(),
                participant.getDepartureAlarmTime(),
                participant.getEstimatedArrival(),
                interval,
                preparationTime,
                flaskResponse != null ? flaskResponse.whichStation() : null,
                flaskResponse != null ? flaskResponse.boardingTime() : null
        );
    }
}
