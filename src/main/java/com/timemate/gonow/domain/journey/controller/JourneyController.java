package com.timemate.gonow.domain.journey.controller;

import com.timemate.gonow.domain.journey.dto.HomeJourneyCreateRequest;
import com.timemate.gonow.domain.journey.dto.HomeJourneyUpdateRequest;
import com.timemate.gonow.domain.journey.dto.JourneyActiveUpdateRequest;
import com.timemate.gonow.domain.journey.dto.JourneyResponse;
import com.timemate.gonow.domain.journey.dto.JourneySaveResponse;
import com.timemate.gonow.domain.journey.dto.PersonalJourneyCreateRequest;
import com.timemate.gonow.domain.journey.dto.PersonalJourneyUpdateRequest;
import com.timemate.gonow.domain.journey.service.JourneyService;
import com.timemate.gonow.global.auth.MemberId;
import com.timemate.gonow.global.response.ApiResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/journeys")
public class JourneyController {
    private final JourneyService journeyService;

    // 개인 여정 생성
    @PostMapping("/personal")
    public ApiResult<JourneySaveResponse> createPersonalJourney(
            @MemberId Long memberId,
            @Valid @RequestBody PersonalJourneyCreateRequest request) {
        JourneySaveResponse response = journeyService.createPersonalJourney(memberId, request);

        return ApiResult.success("개인 알람 생성 완료", response);
    }

    // 개인 여정 수정
    @PutMapping("/personal/{journeyId}")
    public ApiResult<JourneySaveResponse> updatePersonalJourney(
            @MemberId Long memberId,
            @PathVariable Long journeyId,
            @Valid @RequestBody PersonalJourneyUpdateRequest request) {
        JourneySaveResponse response = journeyService.updatePersonalJourney(memberId, journeyId, request);

        return ApiResult.success("개인 알람 수정 완료", response);
    }

    // 귀가 여정 생성 (막차/데드라인 공통)
    @PostMapping("/home")
    public ApiResult<JourneySaveResponse> createHomeJourney(
            @MemberId Long memberId,
            @Valid @RequestBody HomeJourneyCreateRequest request) {
        JourneySaveResponse response = journeyService.createHomeJourney(memberId, request);

        return ApiResult.success("귀가 알람 생성 완료", response);
    }

    // 귀가 여정 수정 (막차/데드라인 공통)
    @PutMapping("/home/{journeyId}")
    public ApiResult<JourneySaveResponse> updateHomeJourney(
            @MemberId Long memberId,
            @PathVariable Long journeyId,
            @Valid @RequestBody HomeJourneyUpdateRequest request) {
        JourneySaveResponse response = journeyService.updateHomeJourney(memberId, journeyId, request);

        return ApiResult.success("귀가 알람 수정 완료", response);
    }

    // 여정 조회 (개인/귀가 공통)
    @GetMapping("/{journeyId}")
    public ApiResult<JourneyResponse> getJourneyDetail(
            @MemberId Long memberId,
            @PathVariable Long journeyId) {
        JourneyResponse response = journeyService.getJourney(memberId, journeyId);

        return ApiResult.success("여정 상세 조회 완료", response);
    }

    // 알람 스위치 ON/OFF (개인/귀가 공통)
    @PatchMapping("/{journeyId}/active")
    public ApiResult<Void> updateActive(
            @MemberId Long memberId,
            @PathVariable Long journeyId,
            @Valid @RequestBody JourneyActiveUpdateRequest request) {
        journeyService.updateActive(memberId, journeyId, request);

        return ApiResult.success("알람 스위치 설정 완료");
    }

    // 여정 삭제 (개인/귀가 공통)
    @DeleteMapping("/{journeyId}")
    public ApiResult<Void> deleteJourney(
            @MemberId Long memberId,
            @PathVariable Long journeyId) {
        journeyService.deleteJourney(memberId, journeyId);

        return ApiResult.success("알람 삭제 완료");
    }
}
