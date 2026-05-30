package com.timemate.gonow.global.scheduler;

import com.timemate.gonow.domain.appointment.repository.ParticipantRepository;
import com.timemate.gonow.domain.journey.repository.JourneyRepository;
import com.timemate.gonow.global.fcm.FcmSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReadyTransitionService {

    private final JourneyRepository journeyRepository;
    private final ParticipantRepository participantRepository;
    private final FcmSender fcmSender;

    @Transactional
    public void transitionToReady(LocalDate today) {
        int todayBit = 1 << (today.getDayOfWeek().getValue() - 1);

        // FCM 토큰별 ID 목록 수집 (벌크 업데이트 전에 조회해야 SCHEDULED/ARRIVED 상태 기준으로 잡힘)
        Map<String, String> tokenToJourneyIds = journeyRepository.findTokenToJourneyIdsForReadyTransition(today, todayBit);
        Map<String, String> tokenToAppointmentIds = participantRepository.findTokenToAppointmentIdsForReadyTransition(today);

        int journeyCount = journeyRepository.bulkUpdateToReady(today, todayBit);
        int participantCount = participantRepository.bulkUpdateToReady(today);
        log.info("[스케줄러] READY 전환 완료 - 여정: {}건, 참가자: {}건", journeyCount, participantCount);

        // 전체 토큰 합집합 수집 후 토큰별 개별 FCM Data 발송
        Set<String> allTokens = new HashSet<>(tokenToJourneyIds.keySet());
        allTokens.addAll(tokenToAppointmentIds.keySet());

        allTokens.forEach(token ->
                fcmSender.sendData(token, createReadyData(token, tokenToJourneyIds, tokenToAppointmentIds))
        );

        log.info("[스케줄러] FCM Data 발송 완료 - {}건", allTokens.size());
    }

    private Map<String, String> createReadyData(String token, Map<String, String> tokenToJourneyIds, Map<String, String> tokenToAppointmentIds) {
        Map<String, String> data = new HashMap<>();

        String journeyIds = tokenToJourneyIds.get(token);
        if (journeyIds != null) data.put("journey_ids", journeyIds);

        String appointmentIds = tokenToAppointmentIds.get(token);
        if (appointmentIds != null) data.put("appointment_ids", appointmentIds);

        return data;
    }
}
