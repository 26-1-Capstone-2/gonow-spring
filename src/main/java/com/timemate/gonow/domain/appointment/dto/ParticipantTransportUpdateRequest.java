package com.timemate.gonow.domain.appointment.dto;

import com.timemate.gonow.domain.common.constant.TransportType;
import jakarta.validation.constraints.NotNull;

public record ParticipantTransportUpdateRequest(
        @NotNull(message = "이동 수단 필수")
        TransportType transportType
) {}
