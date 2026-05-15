package com.timemate.gonow.domain.appointment.dto;

import com.timemate.gonow.domain.common.constant.TransportType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AppointmentJoinRequest(
        @NotBlank(message = "초대 코드 필수")
        String inviteCode,

        @NotNull(message = "이동 수단 필수")
        TransportType transportType
) {}
