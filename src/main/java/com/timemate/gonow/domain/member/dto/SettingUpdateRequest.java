package com.timemate.gonow.domain.member.dto;

import com.timemate.gonow.domain.member.constant.PriorityType;
import com.timemate.gonow.domain.member.constant.TransitType;
import jakarta.validation.constraints.NotNull;

public record SettingUpdateRequest(
        @NotNull(message = "선호 대중교통 필수 (ALL, SUBWAY, BUS)")
        TransitType transitType,

        @NotNull(message = "경로 우선순위 필수 (MIN_TIME, MIN_TRANSFER, MIN_WALK)")
        PriorityType priorityType,

        @NotNull(message = "여유시간 필수")
        Integer preparationTime
) {}
