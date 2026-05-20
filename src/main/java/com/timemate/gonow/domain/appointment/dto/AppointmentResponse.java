package com.timemate.gonow.domain.appointment.dto;

import com.timemate.gonow.domain.appointment.constant.AppointmentStatus;
import com.timemate.gonow.domain.appointment.entity.Appointment;
import com.timemate.gonow.domain.appointment.entity.Participant;
import com.timemate.gonow.domain.common.constant.TransportType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record AppointmentResponse(
        Long appointmentId,
        LocalDate planDate,
        LocalDateTime targetTime,
        String destName,
        String destAddress,
        BigDecimal destLat,
        BigDecimal destLng,
        String inviteCode,
        AppointmentStatus appointmentStatus,
        List<ParticipantInfo> participants
) {
    public record ParticipantInfo(
            Long memberId,
            String nickname,
            TransportType transportType,
            boolean isHost
    ) {}

    public static AppointmentResponse from(Appointment appointment, List<Participant> participants) {
        List<ParticipantInfo> participantInfos = participants.stream()
                .map(p -> new ParticipantInfo(
                        p.getMember().getId(),
                        p.getMember().getNickname(), // fetch join 필요(member.nickname)
                        p.getTransportType(),
                        p.isHost()
                ))
                .toList();

        return new AppointmentResponse(
                appointment.getId(),
                appointment.getPlanDate(),
                appointment.getTargetTime(),
                appointment.getDestination().name(),
                appointment.getDestination().address(),
                appointment.getDestination().point().lat(),
                appointment.getDestination().point().lng(),
                appointment.getInviteCode(),
                appointment.getAppointmentStatus(),
                participantInfos
        );
    }
}
