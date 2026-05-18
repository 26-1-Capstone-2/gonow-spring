package com.timemate.gonow.domain.appointment.dto;

import com.timemate.gonow.domain.appointment.constant.ParticipantStatus;
import com.timemate.gonow.domain.appointment.entity.Participant;

import java.time.LocalDateTime;

public record ParticipantLocationUpdateResponse(
        ParticipantStatus participantStatus,
        LocalDateTime departureAlarmTime,
        LocalDateTime estimatedArrival
) {
    public static ParticipantLocationUpdateResponse from(Participant participant) {
        return new ParticipantLocationUpdateResponse(
                participant.getParticipantStatus(),
                participant.getDepartureAlarmTime(),
                participant.getEstimatedArrival()
        );
    }
}
