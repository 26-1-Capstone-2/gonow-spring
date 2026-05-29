package com.timemate.gonow.domain.member.controller;

import com.timemate.gonow.domain.member.dto.*;
import com.timemate.gonow.domain.member.service.MemberService;
import com.timemate.gonow.global.response.ApiResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.timemate.gonow.global.auth.MemberId;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/members")
public class MemberController {
    private final MemberService memberService;

    // 회원가입
    @PostMapping
    public ApiResult<Void> signUp(@Valid @RequestBody SignupRequest request) {
        memberService.signUp(request);

        return ApiResult.success("회원가입 완료");
    }


    // 이메일 중복 확인
    @GetMapping(value = "/check", params = "email")
    public ApiResult<Void> checkEmailAvailable(@RequestParam String email) {
        memberService.checkEmailAvailable(email);

        return ApiResult.success("사용 가능한 이메일");
    }

    // 닉네임 중복 확인
    @GetMapping(value = "/check", params = "nickname")
    public ApiResult<Void> checkNicknameAvailable(@RequestParam String nickname) {
        memberService.checkNicknameAvailable(nickname);

        return ApiResult.success("사용 가능한 닉네임");
    }

    // 닉네임 변경
    @PatchMapping("/me/nickname")
    public ApiResult<Void> updateNickname(@MemberId Long memberId,
                                          @Valid @RequestBody NicknameUpdateRequest request) {
        memberService.updateNickname(memberId, request);

        return ApiResult.success("닉네임 변경 완료");
    }

    // 비밀번호 변경
    @PatchMapping("/me/password")
    public ApiResult<Void> updatePassword(@MemberId Long memberId,
                                          @Valid @RequestBody PasswordUpdateRequest request) {
        memberService.updatePassword(memberId, request);

        return ApiResult.success("비밀번호 변경 완료");
    }

    // home 등록/변경
    @PatchMapping("/me/home")
    public ApiResult<Void> updateHome(@MemberId Long memberId,
                                      @Valid @RequestBody HomeUpdateRequest request) {
        memberService.updateHome(memberId, request);

        return ApiResult.success("귀가지 설정 완료");
    }

    // 내 프로필 조회
    @GetMapping("/me")
    public ApiResult<MyProfileResponse> getMyProfile(@MemberId Long memberId) {
        MyProfileResponse response = memberService.getMyProfile(memberId);

        return ApiResult.success("프로필 조회 완료", response);
    }

    // FCM 토큰 등록/갱신
    @PatchMapping("/me/fcm-token")
    public ApiResult<Void> updateFcmToken(@MemberId Long memberId,
                                          @Valid @RequestBody FcmTokenUpdateRequest request) {
        memberService.updateFcmToken(memberId, request);

        return ApiResult.success("FCM 토큰 등록 완료");
    }

    // 미완성 -----------------------------------------------------------------------------------
    // 회원탈퇴
    // 현재는 껍데기 — 로그만 찍고 성공 응답 반환.
    // 추후 연관 테이블 확정 후 Hard Delete 로직 채울 예정.
    @DeleteMapping("/me")
    public ApiResult<Void> withdraw(@MemberId Long memberId) {
        log.info("회원 탈퇴 요청 - memberId: {}", memberId);

        // TODO: 추후에 연관 데이터 수동 삭제 로직(Hard Delete)이 들어갈 자리입니다.

        return ApiResult.success("회원 탈퇴 완료");
    }
}
