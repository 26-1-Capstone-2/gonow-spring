package com.timemate.gonow.domain.member.controller;

import com.timemate.gonow.domain.member.dto.*;
import com.timemate.gonow.domain.member.service.MemberService;
import com.timemate.gonow.global.response.ApiResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/members")
public class MemberController {
    private final MemberService memberService;

    // 회원가입
    @PostMapping
    public ApiResult<Long> signUp(@Valid @RequestBody SignupRequest request) {
        Long memberId = memberService.signUp(request);

        return ApiResult.success("회원가입 완료", memberId);
    }

    // 이메일 중복 확인
    @GetMapping(value = "/check", params = "email")
    public ApiResult<Boolean> isEmailAvailable(@RequestParam String email) {
        boolean isAvailable = memberService.isEmailAvailable(email);

        return ApiResult.success("이메일 중복 확인", isAvailable);
    }

    // 닉네임 중복 확인
    @GetMapping(value = "/check", params = "nickname")
    public ApiResult<Boolean> isNicknameAvailable(@RequestParam String nickname) {
        boolean isAvailable = memberService.isNicknameAvailable(nickname);

        return ApiResult.success("닉네임 중복 확인", isAvailable);
    }

    // 닉네임 변경
    @PatchMapping("/me/nickname")
    public ApiResult<Void> updateNickname(@AuthenticationPrincipal UserDetails userDetails,
                                          @Valid @RequestBody NicknameUpdateRequest request) {
        Long memberId = Long.parseLong(userDetails.getUsername());
        memberService.updateNickname(memberId, request);

        return ApiResult.success("닉네임이 변경 완료");
    }

    // 비밀번호 변경
    @PatchMapping("/me/password")
    public ApiResult<Void> updatePassword(@AuthenticationPrincipal UserDetails userDetails,
                                          @Valid @RequestBody PasswordUpdateRequest request) {
        Long memberId = Long.parseLong(userDetails.getUsername());
        memberService.updatePassword(memberId, request);

        return ApiResult.success("비밀번호 변경 완료");
    }

    // 귀가지 등록/변경
    @PatchMapping("/me/home")
    public ApiResult<HomeUpdateResponse> updateHome(@AuthenticationPrincipal UserDetails userDetails,
                                                    @Valid @RequestBody HomeUpdateRequest request) {
        Long memberId = Long.parseLong(userDetails.getUsername());

        HomeUpdateResponse response = memberService.updateHome(memberId, request);

        return ApiResult.success("귀가지 설정 완료", response);
    }

    // 귀가지 삭제
    // 아마 안 쓰일 거 같음
    @DeleteMapping("/me/home")
    public ApiResult<Void> deleteHome(@AuthenticationPrincipal UserDetails userDetails) {
        Long memberId = Long.parseLong(userDetails.getUsername());

        memberService.deleteHome(memberId);

        return ApiResult.success("귀가지가 삭제되었습니다");
    }

    // 내 프로필 조회
    @GetMapping("/me")
    public ApiResult<MyProfileResponse> getMyProfile(@AuthenticationPrincipal UserDetails userDetails) {
        Long memberId = Long.parseLong(userDetails.getUsername());

        MyProfileResponse response = memberService.getMyProfile(memberId);

        return ApiResult.success("프로필 조회 완료", response);
    }

    // 미완성 -----------------------------------------------------------------------------------
    // 회원탈퇴
    // 현재는 껍데기 — 로그만 찍고 성공 응답 반환.
    // 추후 연관 테이블 확정 후 Hard Delete 로직 채울 예정.
    @DeleteMapping("/me")
    public ApiResult<Void> withdraw(@AuthenticationPrincipal UserDetails userDetails) {
        log.info("회원 탈퇴 요청 - memberId: {}", userDetails.getUsername());

        // TODO: 추후에 연관 데이터 수동 삭제 로직(Hard Delete)이 들어갈 자리입니다.

        return ApiResult.success("회원 탈퇴 완료");
    }
}
