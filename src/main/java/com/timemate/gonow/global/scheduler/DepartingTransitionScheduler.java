package com.timemate.gonow.global.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class DepartingTransitionScheduler {

    private final DepartingTransitionService departingTransitionService;

    // 매 1분마다: READY + departureAlarmTime 도달 → DEPARTING
    @Scheduled(cron = "0 * * * * *")
    public void transitionToDeparting() {
        LocalDateTime now = LocalDateTime.now();
        log.info("[스케줄러] READY→DEPARTING 전환 시작 - {}", now);

        try {
            departingTransitionService.transitionToDeparting(now);
        } catch (Exception e) {
            log.error("[스케줄러] READY→DEPARTING 전환 실패 - {}", now, e);
        }
    }
}
