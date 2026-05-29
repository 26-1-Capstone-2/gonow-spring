package com.timemate.gonow.global.scheduler;

import com.timemate.gonow.domain.appointment.repository.ParticipantRepository;
import com.timemate.gonow.domain.journey.repository.JourneyRepository;
import com.timemate.gonow.global.fcm.FcmSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReadyTransitionService {

    private final JourneyRepository journeyRepository;
    private final ParticipantRepository participantRepository;
    private final FcmSender fcmSender;

    // 하나의 트랜잭션으로 Journey + Participant 동시 전환 후 FCM Data 발송
    @Transactional
    public void transitionToReady(LocalDate today) {
        int todayBit = 1 << (today.getDayOfWeek().getValue() - 1);

        // FCM 토큰 수집 (벌크 업데이트 전에 조회해야 SCHEDULED/ARRIVED 상태 기준으로 잡힘)
        List<String> journeyTokens = journeyRepository.findFcmTokensForReadyTransition(today, todayBit);
        List<String> participantTokens = participantRepository.findFcmTokensForReadyTransition(today);

        int journeyCount = journeyRepository.bulkUpdateToReady(today, todayBit);
        int participantCount = participantRepository.bulkUpdateToReady(today);
        log.info("[스케줄러] READY 전환 완료 - 여정: {}건, 참가자: {}건", journeyCount, participantCount);

        // 중복 제거 후 FCM Data 다중 발송 (앱에서 GPS 가동 트리거)
        Set<String> uniqueTokens = new HashSet<>(journeyTokens);
        uniqueTokens.addAll(participantTokens);

        fcmSender.sendAllData(uniqueTokens, "READY");
        log.info("[스케줄러] FCM Data 발송 완료 - {}건", uniqueTokens.size());
    }
}
