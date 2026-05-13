package com.timemate.gonow.domain.appointment.controller;

import com.timemate.gonow.domain.appointment.dto.ParticipantActiveUpdateRequest;
import com.timemate.gonow.domain.appointment.service.ParticipantService;
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

    // 탈퇴(본인) & 추방(방장) 공통
    @DeleteMapping("/{targetMemberId}")
    public ApiResult<Void> deleteParticipant(
            @MemberId Long memberId,
            @PathVariable Long appointmentId,
            @PathVariable Long targetMemberId) {
        participantService.deleteParticipant(memberId, appointmentId, targetMemberId);

        return ApiResult.success("참가자 삭제 완료");
    }
}
