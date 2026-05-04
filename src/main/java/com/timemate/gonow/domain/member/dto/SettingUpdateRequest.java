package com.timemate.gonow.domain.member.dto;

import com.timemate.gonow.domain.member.constant.PriorityType;
import com.timemate.gonow.domain.member.constant.TransitType;
import jakarta.validation.constraints.NotNull;

public record SettingUpdateRequest(
        @NotNull(message = "선호 대중교통은 필수입니다.")
        TransitType transitType,

        @NotNull(message = "경로 우선순위는 필수입니다.")
        PriorityType priorityType
) {}
