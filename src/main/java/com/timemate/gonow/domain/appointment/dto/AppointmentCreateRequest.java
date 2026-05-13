package com.timemate.gonow.domain.appointment.dto;

import com.timemate.gonow.domain.common.constant.TransportType;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record AppointmentCreateRequest(
        String title,

        @NotNull(message = "약속 예정 날짜 필수")
        @FutureOrPresent(message = "약속 예정 날짜는 오늘 이후여야 합니다.")
        LocalDate planDate,

        @NotNull(message = "목표 시간 필수")
        @FutureOrPresent(message = "목표 시간은 현재 시각 이후여야 합니다.")
        LocalDateTime targetTime,

        @NotBlank(message = "목적지 이름 필수") String destName,
        @NotBlank(message = "목적지 주소 필수") String destAddress,
        @NotNull(message = "목적지 위도 필수") BigDecimal destLat,
        @NotNull(message = "목적지 경도 필수") BigDecimal destLng,

        @NotNull(message = "이동 수단 필수")
        TransportType transportType
) {}
