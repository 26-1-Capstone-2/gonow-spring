package com.timemate.gonow.domain.appointment.dto;

import jakarta.validation.constraints.NotNull;

public record ParticipantActiveUpdateRequest(
        @NotNull(message = "참가자 알람 활성화 여부 필수")
        Boolean isActive
) {}
