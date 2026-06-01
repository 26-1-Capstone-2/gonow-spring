package com.timemate.gonow.global.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class ArrivedTransitionScheduler {

    private final ArrivedTransitionService arrivedTransitionService;

    // 매 1분마다: targetTime 초과 → 자동 ARRIVED
    // - NEARDEST: targetTime 즉시 ARRIVED
    // - READY/DEPARTING/MOVING: targetTime+1시간 후 ARRIVED (지각 1시간 여유)
    @Scheduled(cron = "0 * * * * *")
    public void transitionToArrived() {
        LocalDateTime now = LocalDateTime.now();
        log.info("[스케줄러] 자동 ARRIVED 전환 시작 - {}", now);

        try {
            arrivedTransitionService.transitionNeardestToArrived(now);
        } catch (Exception e) {
            log.error("[스케줄러] NEARDEST 자동 ARRIVED 전환 실패 - {}", now, e);
        }

        try {
            arrivedTransitionService.transitionActiveToArrived(now);
        } catch (Exception e) {
            log.error("[스케줄러] 지각 자동 ARRIVED 전환 실패 - {}", now, e);
        }
    }
}
