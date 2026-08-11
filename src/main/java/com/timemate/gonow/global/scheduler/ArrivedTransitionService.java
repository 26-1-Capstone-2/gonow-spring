package com.timemate.gonow.global.scheduler;

import com.timemate.gonow.domain.appointment.repository.AppointmentRepository;
import com.timemate.gonow.domain.appointment.repository.ParticipantRepository;
import com.timemate.gonow.domain.journey.repository.JourneyRepository;
import com.timemate.gonow.global.fcm.FcmSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArrivedTransitionService {

    private final JourneyRepository journeyRepository;
    private final ParticipantRepository participantRepository;
    private final AppointmentRepository appointmentRepository;
    private final FcmSender fcmSender;

    @Value("${scheduler.day-boundary-hour}")
    private int dayBoundaryHour;

    // NEARDEST + targetTime 초과 → 즉시 ARRIVED
    @Transactional
    public void transitionNeardestToArrived(LocalDateTime now) {
        // 자정~새벽 dayBoundaryHour 이전은 어제 날짜로 보정 (막차 모드 자정 넘김 커버)
        LocalDate planDate = now.getHour() < dayBoundaryHour ? now.toLocalDate().minusDays(1) : now.toLocalDate();
        LocalDate today = now.toLocalDate();
        int planDateBit = 1 << (planDate.getDayOfWeek().getValue() - 1);

        // FCM 토큰별 ID 목록 수집 (벌크 업데이트 전에 조회해야 NEARDEST 상태 기준으로 잡힘)
        // NEARDEST가 지오펜싱 기반이 된 뒤로는 클라이언트가 이 자동 전환을 스스로 알 방법이 없음
        // -> FCM으로 알려줘서 트리거한다.
        Map<String, String> tokenToJourneyIds = journeyRepository.findTokenToJourneyIdsForNeardestOverdue(planDate, now, planDateBit);
        Map<String, String> tokenToAppointmentIds = participantRepository.findTokenToAppointmentIdsForNeardestOverdue(today, now);

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

        // 전체 토큰 합집합 수집 후 토큰별 개별 FCM Data 발송
        Set<String> allTokens = new HashSet<>(tokenToJourneyIds.keySet());
        allTokens.addAll(tokenToAppointmentIds.keySet());
        if (!allTokens.isEmpty()) {
            allTokens.forEach(token ->
                    fcmSender.sendData(token, createAutoArrivedData(token, tokenToJourneyIds, tokenToAppointmentIds))
            );
            log.info("[스케줄러] NEARDEST 자동 ARRIVED FCM Data 발송 완료 - {}건", allTokens.size());
        }
    }

    private Map<String, String> createAutoArrivedData(String token, Map<String, String> tokenToJourneyIds, Map<String, String> tokenToAppointmentIds) {
        Map<String, String> data = new HashMap<>();
        data.put("sync_event", "auto_arrived");

        String journeyIds = tokenToJourneyIds.get(token);
        if (journeyIds != null) data.put("journey_ids", journeyIds);

        String appointmentIds = tokenToAppointmentIds.get(token);
        if (appointmentIds != null) data.put("appointment_ids", appointmentIds);

        return data;
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
