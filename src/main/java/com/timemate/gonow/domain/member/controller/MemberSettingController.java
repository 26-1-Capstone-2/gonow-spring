package com.timemate.gonow.domain.member.controller;

import com.timemate.gonow.domain.member.dto.SettingUpdateRequest;
import com.timemate.gonow.domain.member.dto.SettingUpdateResponse;
import com.timemate.gonow.domain.member.service.MemberSettingService;
import com.timemate.gonow.global.response.SuccessResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/members")
public class MemberSettingController {
    private final MemberSettingService memberSettingService;

    // 멤버 설정 등록/수정
    @PatchMapping("/me/setting")
    public SuccessResult<SettingUpdateResponse> updateSetting(@AuthenticationPrincipal UserDetails userDetails,
                                                              @Valid @RequestBody SettingUpdateRequest request) {
        Long memberId = Long.parseLong(userDetails.getUsername());

        SettingUpdateResponse response = memberSettingService.updateSetting(memberId, request);

        return SuccessResult.of("멤버 설정이 완료되었습니다.", response);
    }
}
