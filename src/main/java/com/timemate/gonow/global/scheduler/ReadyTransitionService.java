package com.timemate.gonow.global.scheduler;

import com.timemate.gonow.domain.appointment.repository.ParticipantRepository;
import com.timemate.gonow.domain.journey.repository.JourneyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReadyTransitionService {

    private final JourneyRepository journeyRepository;
    private final ParticipantRepository participantRepository;

    // 하나의 트랜잭션으로 Journey + Participant 동시 전환
    @Transactional
    public void transitionToReady(LocalDate today) {
        int todayBit = 1 << (today.getDayOfWeek().getValue() - 1);
        int journeyCount = journeyRepository.bulkUpdateToReady(today, todayBit);

        int participantCount = participantRepository.bulkUpdateToReady(today);
        log.info("[스케줄러] READY 전환 완료 - 여정: {}건, 참가자: {}건", journeyCount, participantCount);
    }
}
