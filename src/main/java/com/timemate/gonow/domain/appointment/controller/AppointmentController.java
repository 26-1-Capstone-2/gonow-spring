package com.timemate.gonow.domain.appointment.controller;

import com.timemate.gonow.domain.appointment.dto.AppointmentCreateRequest;
import com.timemate.gonow.domain.appointment.dto.AppointmentCreateResponse;
import com.timemate.gonow.domain.appointment.dto.AppointmentJoinRequest;
import com.timemate.gonow.domain.appointment.dto.AppointmentJoinResponse;
import com.timemate.gonow.domain.appointment.dto.AppointmentResponse;
import com.timemate.gonow.domain.appointment.dto.AppointmentUpdateRequest;
import com.timemate.gonow.domain.appointment.dto.AppointmentUpdateResponse;
import com.timemate.gonow.domain.appointment.dto.DashboardResponse;
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

    // 초대코드로 참여
    @PostMapping("/join")
    public ApiResult<AppointmentJoinResponse> joinAppointment(
            @MemberId Long memberId,
            @Valid @RequestBody AppointmentJoinRequest request) {
        AppointmentJoinResponse response = appointmentService.joinAppointment(memberId, request);

        return ApiResult.success("약속 참여 완료", response);
    }

    // 그룹 알람 조회
    @GetMapping("/{appointmentId}")
    public ApiResult<AppointmentResponse> getAppointment(
            @MemberId Long memberId,
            @PathVariable Long appointmentId) {
        AppointmentResponse response = appointmentService.getAppointment(memberId, appointmentId);

        return ApiResult.success("그룹 알람 상세 조회 완료", response);
    }

    // 도착 예정 대시보드 조회
    @GetMapping("/{appointmentId}/dashboard")
    public ApiResult<DashboardResponse> getDashboard(
            @MemberId Long memberId,
            @PathVariable Long appointmentId) {
        DashboardResponse response = appointmentService.getDashboard(memberId, appointmentId);

        return ApiResult.success("대시보드 조회 완료", response);
    }

    // 그룹 알람 수정 (방장 전용)
    @PatchMapping("/{appointmentId}")
    public ApiResult<AppointmentUpdateResponse> updateAppointment(
            @MemberId Long memberId,
            @PathVariable Long appointmentId,
            @Valid @RequestBody AppointmentUpdateRequest request) {
        AppointmentUpdateResponse response = appointmentService.updateAppointment(memberId, appointmentId, request);

        return ApiResult.success("그룹 알람 수정 완료", response);
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
