package com.timemate.gonow.global.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReadyTransitionScheduler {
    private final ReadyTransitionService readyTransitionService;

    // 매일 새벽 4시: 당일 SCHEDULED → READY 일괄 전환 (scheduler.ready-transition-cron)
    // 반복 여정도 포함
    @Scheduled(cron = "${scheduler.ready-transition-cron}")
    public void transitionToReady() {
        LocalDate today = LocalDate.now();
        log.info("[스케줄러] READY 전환 시작 - {}", today);

        try {
            readyTransitionService.transitionToReady(today);
            // TODO: FCM 발송 ("오늘 약속 있어요" 알림)
        } catch (Exception e) {
            log.error("[스케줄러] READY 전환 실패 - {}", today, e);
        }
    }
}
