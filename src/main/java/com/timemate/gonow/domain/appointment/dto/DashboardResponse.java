package com.timemate.gonow.domain.appointment.dto;

import com.timemate.gonow.domain.appointment.entity.Appointment;
import com.timemate.gonow.domain.appointment.entity.Participant;
import com.timemate.gonow.domain.common.constant.TransportType;

import java.time.LocalDateTime;
import java.util.List;

public record DashboardResponse(
        LocalDateTime targetTime,
        String destName,
        List<ParticipantDashboard> participants
) {
    public record ParticipantDashboard(
            String nickname,
            TransportType transportType,
            LocalDateTime estimatedArrival,
            boolean isMe
    ) {}

    public static DashboardResponse from(Appointment appointment, List<Participant> participants, Long myMemberId) {
        List<ParticipantDashboard> participantDashboards = participants.stream()
                .map(p -> new ParticipantDashboard(
                        p.getMember().getNickname(),
                        p.getTransportType(),
                        p.getEstimatedArrival(),
                        p.getMember().getId().equals(myMemberId)
                ))
                .toList();

        return new DashboardResponse(
                appointment.getTargetTime(),
                appointment.getDestination().name(),
                participantDashboards
        );
    }
}
