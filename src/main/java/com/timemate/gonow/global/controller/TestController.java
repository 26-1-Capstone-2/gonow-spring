package com.timemate.gonow.global.controller;

import com.timemate.gonow.global.response.ApiResult;
import com.timemate.gonow.global.scheduler.ReadyTransitionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

// TODO: 테스트 완료 후 이 파일 삭제
@RestController
@RequiredArgsConstructor
public class TestController {

    private final ReadyTransitionService readyTransitionService;

    @PostMapping("/internal/scheduler/ready")
    public ApiResult<Void> triggerReadyTransition() {
        readyTransitionService.transitionToReady(LocalDate.now());
        return ApiResult.success("READY 전환 + FCM Data 발송 완료");
    }
}
