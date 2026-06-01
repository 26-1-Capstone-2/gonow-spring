package com.timemate.gonow.global.scheduler;

import com.timemate.gonow.domain.appointment.repository.AppointmentRepository;
import com.timemate.gonow.domain.appointment.repository.ParticipantRepository;
import com.timemate.gonow.domain.journey.repository.JourneyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArrivedTransitionService {

    private final JourneyRepository journeyRepository;
    private final ParticipantRepository participantRepository;
    private final AppointmentRepository appointmentRepository;

    @Value("${scheduler.day-boundary-hour}")
    private int dayBoundaryHour;

    // NEARDEST + targetTime 초과 → 즉시 ARRIVED
    @Transactional
    public void transitionNeardestToArrived(LocalDateTime now) {
        // 자정~새벽 dayBoundaryHour 이전은 어제 날짜로 보정 (막차 모드 자정 넘김 커버)
        LocalDate planDate = now.getHour() < dayBoundaryHour ? now.toLocalDate().minusDays(1) : now.toLocalDate();
        LocalDate today = now.toLocalDate();
        int planDateBit = 1 << (planDate.getDayOfWeek().getValue() - 1);

        // [Journey] NEARDEST + targetTime 초과 → ARRIVED
        List<Long> journeyIds = journeyRepository.findIdsNeardestOverdue(planDate, now, planDateBit);
        if (!journeyIds.isEmpty()) {
            int count = journeyRepository.bulkUpdateToArrived(journeyIds);
            log.info("[스케줄러] 여정 NEARDEST 자동 ARRIVED 전환 완료 - {}건", count);
        }

        // [Participant] NEARDEST + targetTime 초과 → ARRIVED + 약속 상태 동기화
        List<Long> appointmentIds = participantRepository.findAppointmentIdsWithOverdueParticipants(today, now);
        if (!appointmentIds.isEmpty()) {
            int count = participantRepository.bulkUpdateToArrivedByAppointmentIds(appointmentIds);
            log.info("[스케줄러] 참가자 NEARDEST 자동 ARRIVED 전환 완료 - {}건", count);
            appointmentRepository.bulkUpdateToActive(appointmentIds);
            appointmentRepository.bulkUpdateToFinished(appointmentIds);
            log.info("[스케줄러] 약속 상태 동기화 완료 - {}건", appointmentIds.size());
        }
    }

    // READY/DEPARTING/MOVING + targetTime+1시간 초과 → ARRIVED (지각 1시간 여유)
    @Transactional
    public void transitionActiveToArrived(LocalDateTime now) {
        LocalDate planDate = now.getHour() < dayBoundaryHour ? now.toLocalDate().minusDays(1) : now.toLocalDate();
        LocalDate today = now.toLocalDate();
        LocalDateTime oneHourAgo = now.minusHours(1);
        int planDateBit = 1 << (planDate.getDayOfWeek().getValue() - 1);

        // [Journey] READY/DEPARTING/MOVING + targetTime+1시간 초과 → ARRIVED
        List<Long> journeyIds = journeyRepository.findIdsActiveOverdue(planDate, oneHourAgo, planDateBit);
        if (!journeyIds.isEmpty()) {
            int count = journeyRepository.bulkUpdateToArrived(journeyIds);
            log.info("[스케줄러] 여정 지각 자동 ARRIVED 전환 완료 - {}건", count);
        }

        // [Participant] READY/DEPARTING/MOVING + targetTime+1시간 초과 → ARRIVED + 약속 상태 동기화
        List<Long> appointmentIds = participantRepository.findAppointmentIdsWithActiveOverdueParticipants(today, oneHourAgo);
        if (!appointmentIds.isEmpty()) {
            int count = participantRepository.bulkUpdateActiveToArrivedByAppointmentIds(appointmentIds);
            log.info("[스케줄러] 참가자 지각 자동 ARRIVED 전환 완료 - {}건", count);
            appointmentRepository.bulkUpdateToActive(appointmentIds);
            appointmentRepository.bulkUpdateToFinished(appointmentIds);
            log.info("[스케줄러] 약속 상태 동기화 완료 - {}건", appointmentIds.size());
        }
    }
}
