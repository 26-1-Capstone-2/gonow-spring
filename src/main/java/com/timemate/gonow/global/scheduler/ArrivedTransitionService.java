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

    // 매 1분마다: NEARDEST + targetTime 초과 → 자동 ARRIVED 벌크 업데이트
    @Transactional
    public void transitionToArrived(LocalDateTime now) {
        // 자정~새벽 dayBoundaryHour 이전은 어제 날짜로 보정 (막차 모드 자정 넘김 커버)
        LocalDate planDate = now.getHour() < dayBoundaryHour ? now.toLocalDate().minusDays(1) : now.toLocalDate();
        LocalDate today = now.toLocalDate();

        // [Journey] NEARDEST + targetTime 초과 → ARRIVED
        int planDateBit = 1 << (planDate.getDayOfWeek().getValue() - 1);
        List<Long> journeyIds = journeyRepository.findIdsNeardestOverdue(planDate, now, planDateBit);
        if (!journeyIds.isEmpty()) {
            int count = journeyRepository.bulkUpdateToArrived(journeyIds);
            log.info("[스케줄러] 여정 자동 ARRIVED 전환 완료 - {}건", count);
        }

        // [Participant] 1단계: 영향받는 약속 ID 목록 추출
        List<Long> appointmentIds = participantRepository.findAppointmentIdsWithOverdueParticipants(today, now);

        if (!appointmentIds.isEmpty()) {
            // 2단계: 해당 약속의 NEARDEST 참가자 → ARRIVED 벌크 전환
            int count = participantRepository.bulkUpdateToArrivedByAppointmentIds(appointmentIds, now);
            log.info("[스케줄러] 참가자 자동 ARRIVED 전환 완료 - {}건", count);

            // 3단계: Appointment 상태 동기화
            appointmentRepository.bulkUpdateToActive(appointmentIds);
            appointmentRepository.bulkUpdateToFinished(appointmentIds);
            log.info("[스케줄러] 약속 상태 동기화 완료 - {}건", appointmentIds.size());
        }
    }
}
