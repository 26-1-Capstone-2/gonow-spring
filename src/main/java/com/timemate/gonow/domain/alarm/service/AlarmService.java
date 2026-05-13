package com.timemate.gonow.domain.alarm.service;

import com.timemate.gonow.domain.alarm.constant.AlarmType;
import com.timemate.gonow.domain.alarm.dto.AlarmResponse;
import com.timemate.gonow.domain.appointment.entity.Appointment;
import com.timemate.gonow.domain.appointment.entity.Participant;
import com.timemate.gonow.domain.appointment.repository.ParticipantRepository;
import com.timemate.gonow.domain.journey.constant.JourneyType;
import com.timemate.gonow.domain.journey.entity.Journey;
import com.timemate.gonow.domain.journey.repository.JourneyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AlarmService {
    private final JourneyRepository journeyRepository;
    private final ParticipantRepository participantRepository;

    // 날짜별 조회 (개인 + 귀가 + 그룹 혼합)
    public List<AlarmResponse> getAlarmsByDate(Long memberId, LocalDate date) {
        List<AlarmResponse> result = new ArrayList<>();

        // 날짜별 개인 + 귀가 알람 조회
        journeyRepository.findAllByPlanDate(memberId, date)
                .forEach(j -> result.add(toJourneyResponse(j)));

        // 날짜별 그룹 알람 조회
        participantRepository.findAllByMemberIdAndPlanDate(memberId, date)
                .forEach(p -> result.add(toAppointmentResponse(p)));

        // 합쳐서 반환
        return result;
    }

    // 타입별 전체 조회
    public List<AlarmResponse> getAlarmsByType(Long memberId, AlarmType type) {
        return switch (type) {
            case PERSONAL -> journeyRepository
                    // 개인 알람 전체 조회
                    .findAllByJourneyType(memberId, JourneyType.PERSONAL)
                    .stream().map(this::toJourneyResponse).toList();
            case HOME -> journeyRepository
                    // 귀가 알람 전체 조회
                    .findAllByJourneyType(memberId, JourneyType.HOME)
                    .stream().map(this::toJourneyResponse).toList();
            case GROUP -> participantRepository
                    .findAllByMemberId(memberId)
                    .stream().map(this::toAppointmentResponse).toList();
        };
    }

    private AlarmResponse toJourneyResponse(Journey journey) {
        AlarmType alarmType = journey.getJourneyType() == JourneyType.PERSONAL ? AlarmType.PERSONAL : AlarmType.HOME;

        return new AlarmResponse(
                alarmType,
                journey.getId(),
                null,
                journey.getDestination().name(),
                journey.getPlanDate(),
                journey.getTargetTime(),
                journey.getDepartureAlarmTime(),
                journey.getTransportType(),
                journey.isActive(),
                journey.isLastMode(),
                journey.getRepeatDays(),
                null,
                null
        );
    }

    private AlarmResponse toAppointmentResponse(Participant participant) {
        Appointment appointment = participant.getAppointment();
        int participantCount = participantRepository.countByAppointmentId(appointment.getId());

        return new AlarmResponse(
                AlarmType.GROUP,
                null,
                appointment.getId(),
                appointment.getDestination().name(),
                appointment.getPlanDate(),
                appointment.getTargetTime(),
                participant.getDepartureAlarmTime(),
                participant.getTransportType(),
                participant.isActive(),
                false,
                null,
                appointment.getAppointmentStatus(),
                participantCount
        );
    }
}
