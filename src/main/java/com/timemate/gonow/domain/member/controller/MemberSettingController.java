package com.timemate.gonow.domain.member.controller;

import com.timemate.gonow.domain.member.dto.SettingUpdateRequest;
import com.timemate.gonow.domain.member.service.MemberSettingService;
import com.timemate.gonow.global.response.ApiResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import com.timemate.gonow.global.auth.MemberId;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/members")
public class MemberSettingController {
    private final MemberSettingService memberSettingService;

    // 멤버 설정 변경
    @PatchMapping("/me/setting")
    public ApiResult<Void> updateSetting(@MemberId Long memberId,
                                         @Valid @RequestBody SettingUpdateRequest request) {
        memberSettingService.updateSetting(memberId, request);

        return ApiResult.success("설정 완료");
    }
}
