package com.timemate.gonow.global.scheduler;

import com.timemate.gonow.domain.appointment.repository.ParticipantRepository;
import com.timemate.gonow.domain.journey.repository.JourneyRepository;
import com.timemate.gonow.global.fcm.FcmSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

// READY → DEPARTING 시간 트리거(P >= Q) 전용 스케줄러 서비스
@Slf4j
@Service
@RequiredArgsConstructor
public class DepartingTransitionService {

    private final JourneyRepository journeyRepository;
    private final ParticipantRepository participantRepository;
    private final FcmSender fcmSender;

    // READY + departureAlarmTime 도달 → DEPARTING (좌표 없이 상태만 전환)
    @Transactional
    public void transitionToDeparting(LocalDateTime now) {
        // FCM 토큰별 ID 목록 수집 (벌크 업데이트 전에 조회해야 READY 상태 기준으로 잡힘)
        Map<String, String> tokenToJourneyIds = journeyRepository.findTokenToJourneyIdsForDepartingTransition(now);
        Map<String, String> tokenToAppointmentIds = participantRepository.findTokenToAppointmentIdsForDepartingTransition(now);

        // READY + departureAlarmTime 도달 → DEPARTING 벌크 전환.
        int journeyCount = journeyRepository.bulkUpdateToDeparting(now);
        int participantCount = participantRepository.bulkUpdateToDeparting(now);
        log.info("[스케줄러] READY → DEPARTING 전환 완료 - 여정: {}건, 참가자: {}건", journeyCount, participantCount);

        // 전체 토큰 합집합 수집 후 토큰별 개별 FCM Data 발송
        // 클라이언트가 이 신호를 받으면 READY용 지오펜스를 내리고 DEPARTING용 지오펜스를 등록한다.
        Set<String> allTokens = new HashSet<>(tokenToJourneyIds.keySet());
        allTokens.addAll(tokenToAppointmentIds.keySet());

        // if문으로 감싸지 않아도 로직에는 전혀 영향이 없지만, 불필요한 log 출력이 너무 많이 쌓일 수 있어서 차단함
        if (!allTokens.isEmpty()) {
            allTokens.forEach(token ->
                    fcmSender.sendData(token, createDepartingTransitionData(token, tokenToJourneyIds, tokenToAppointmentIds))
            );
            log.info("[스케줄러] READY → DEPARTING 전환 FCM Data 발송 완료 - {}건", allTokens.size());
        }
    }

    private Map<String, String> createDepartingTransitionData(String token, Map<String, String> tokenToJourneyIds, Map<String, String> tokenToAppointmentIds) {
        Map<String, String> data = new HashMap<>();
        data.put("sync_event", "departing_transition");

        String journeyIds = tokenToJourneyIds.get(token);
        if (journeyIds != null) data.put("journey_ids", journeyIds);

        String appointmentIds = tokenToAppointmentIds.get(token);
        if (appointmentIds != null) data.put("appointment_ids", appointmentIds);

        return data;
    }
}
