package com.timemate.gonow.domain.journey.dto;

import jakarta.validation.constraints.NotNull;

public record JourneyActiveUpdateRequest(
        @NotNull(message = "알람 활성화 여부 필수")
        Boolean isActive
) {}
