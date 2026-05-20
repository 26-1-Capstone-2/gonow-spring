package com.timemate.gonow.domain.appointment.controller;

import com.timemate.gonow.domain.appointment.dto.ParticipantActiveUpdateRequest;
import com.timemate.gonow.domain.appointment.dto.ParticipantArriveResponse;
import com.timemate.gonow.domain.appointment.dto.ParticipantLocationUpdateResponse;
import com.timemate.gonow.domain.appointment.dto.ParticipantTransportUpdateRequest;
import com.timemate.gonow.domain.appointment.service.ParticipantService;
import com.timemate.gonow.domain.common.dto.LocationUpdateRequest;
import com.timemate.gonow.global.auth.MemberId;
import com.timemate.gonow.global.response.ApiResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/appointments/{appointmentId}/participants")
public class ParticipantController {
    private final ParticipantService participantService;

    // 참가자 알람 ON/OFF (본인만)
    @PatchMapping("/active")
    public ApiResult<Void> updateParticipantActive(
            @MemberId Long memberId,
            @PathVariable Long appointmentId,
            @Valid @RequestBody ParticipantActiveUpdateRequest request) {
        participantService.updateActive(memberId, appointmentId, request);

        return ApiResult.success("참가자 알람 스위치 설정 완료");
    }

    // 이동 수단 변경 (일반 참가자 전용)
    @PatchMapping("/transport")
    public ApiResult<Void> updateTransportType(
            @MemberId Long memberId,
            @PathVariable Long appointmentId,
            @Valid @RequestBody ParticipantTransportUpdateRequest request) {
        participantService.updateTransportType(memberId, appointmentId, request);

        return ApiResult.success("참가자 알람 수정 완료");
    }

    // 탈퇴(본인) & 추방(방장) 공통
    @DeleteMapping("/{targetMemberId}")
    public ApiResult<Void> deleteParticipant(
            @MemberId Long memberId,
            @PathVariable Long appointmentId,
            @PathVariable Long targetMemberId) {
        participantService.deleteParticipant(memberId, appointmentId, targetMemberId);

        return ApiResult.success("참가자 삭제 완료");
    }


    // GPS 좌표 수신 + 상태 전이
    @PatchMapping("/location")
    public ApiResult<ParticipantLocationUpdateResponse> updateLocation(
            @MemberId Long memberId,
            @PathVariable Long appointmentId,
            @Valid @RequestBody LocationUpdateRequest request) {
        ParticipantLocationUpdateResponse response = participantService.updateLocation(memberId, appointmentId, request);

        return ApiResult.success("위치 업데이트 완료", response);
    }

    // 도착 확인 (NEARDEST 상태에서 사용자 확인 시 ARRIVED 전환)
    @PatchMapping("/arrive")
    public ApiResult<ParticipantArriveResponse> arrive(
            @MemberId Long memberId,
            @PathVariable Long appointmentId) {
        ParticipantArriveResponse response = participantService.arrive(memberId, appointmentId);

        return ApiResult.success("도착 확인 완료", response);
    }
}
