package com.timemate.gonow.domain.appointment.controller;

import com.timemate.gonow.domain.appointment.dto.AppointmentCreateRequest;
import com.timemate.gonow.domain.appointment.dto.AppointmentCreateResponse;
import com.timemate.gonow.domain.appointment.service.AppointmentService;
import com.timemate.gonow.global.auth.MemberId;
import com.timemate.gonow.global.response.ApiResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/appointments")
public class AppointmentController {
    private final AppointmentService appointmentService;

    // 그룹 알람 생성
    @PostMapping
    public ApiResult<AppointmentCreateResponse> createAppointment(
            @MemberId Long memberId,
            @Valid @RequestBody AppointmentCreateRequest request) {
        AppointmentCreateResponse response = appointmentService.createAppointment(memberId, request);

        return ApiResult.success("그룹 알람 생성 완료", response);
    }

    // 그룹 알람 삭제
    @DeleteMapping("/{appointmentId}")
    public ApiResult<Void> deleteAppointment(
            @MemberId Long memberId,
            @PathVariable Long appointmentId) {

        appointmentService.deleteAppointment(memberId, appointmentId);

        return ApiResult.success("알람 삭제 완료");
    }
}
