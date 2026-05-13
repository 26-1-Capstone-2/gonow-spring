package com.timemate.gonow.domain.alarm.controller;

import com.timemate.gonow.domain.alarm.constant.AlarmType;
import com.timemate.gonow.domain.alarm.dto.AlarmResponse;
import com.timemate.gonow.domain.alarm.service.AlarmService;
import com.timemate.gonow.global.auth.MemberId;
import com.timemate.gonow.global.response.ApiResult;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/alarms")
public class AlarmController {
    private final AlarmService alarmService;

    @GetMapping
    public ApiResult<List<AlarmResponse>> getAlarms(
            @MemberId Long memberId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) AlarmType type) {

        if (date == null && type == null) {
            throw new IllegalArgumentException("date 또는 type 중 하나는 필수입니다.");
        }
        if (date != null && type != null) {
            throw new IllegalArgumentException("date와 type은 동시에 사용할 수 없습니다.");
        }

        List<AlarmResponse> response = (date != null)
                ? alarmService.getAlarmsByDate(memberId, date)
                : alarmService.getAlarmsByType(memberId, type);

        return ApiResult.success("알람 조회 완료", response);
    }
}
