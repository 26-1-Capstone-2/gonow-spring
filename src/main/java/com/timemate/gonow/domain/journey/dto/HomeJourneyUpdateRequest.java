package com.timemate.gonow.domain.journey.dto;

import com.timemate.gonow.domain.common.constant.TransportType;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record HomeJourneyUpdateRequest(
        String title,

        @NotNull(message = "막차 여부 필수")
        Boolean isLastMode,

        @NotNull(message = "여정 예정 날짜 필수")
        @FutureOrPresent(message = "여정 예정 날짜는 오늘 이후여야 합니다.")
        LocalDate planDate,

        // 막차 모드(isLastMode=true)에서는 생략 가능 — 플라스크가 READY 첫 GPS 수신 시 막차 시각 계산 후 확정
        // 데드라인 모드(isLastMode=false)에서는 필수 — JourneyService에서 검증
        @FutureOrPresent(message = "목표 시간은 현재 시각 이후여야 합니다.")
        LocalDateTime targetTime,

        @NotBlank(message = "목적지 이름 필수")
        String destName,

        @NotBlank(message = "목적지 주소 필수")
        String destAddress,

        @NotNull(message = "목적지 위도 필수")
        BigDecimal destLat,

        @NotNull(message = "목적지 경도 필수")
        BigDecimal destLng,

        // 막차 모드(isLastMode=true)면 생략 가능 — 서버가 TRANSIT으로 강제
        // 데드라인 모드(isLastMode=false)면 필수
        TransportType transportType,

        @NotNull(message = "반복 요일 필수")
        Integer repeatDays
) {}
