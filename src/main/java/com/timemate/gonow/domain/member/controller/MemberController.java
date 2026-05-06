package com.timemate.gonow.domain.member.controller;

import com.timemate.gonow.domain.member.dto.*;
import com.timemate.gonow.domain.member.service.MemberService;
import com.timemate.gonow.global.response.SuccessResult;
import jakarta.validation.Valid;
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
    public SuccessResult<Long> signUp(@Valid @RequestBody SignupRequest request) {
        Long memberId = memberService.signUp(request);

        return SuccessResult.of("회원가입이 완료되었습니다", memberId);
    }


    // 사용 중인 이메일 확인
    @GetMapping("/email/check")
    public SuccessResult<Boolean> isEmailAvailable(@RequestParam String email) {
        boolean isAvailable = memberService.isEmailAvailable(email);
        return SuccessResult.of("이메일 중복 확인 완료", isAvailable);
    }

    // 사용 중인 닉네임 확인
    @GetMapping("/nickname/check")
    public SuccessResult<Boolean> isNicknameAvailable(@RequestParam String nickname) {
        boolean isAvailable = memberService.isNicknameAvailable(nickname);

        return SuccessResult.of("닉네임 중복 확인 완료", isAvailable);
    }


    // 닉네임 변경
    @PatchMapping("/me/nickname")
    public SuccessResult<Void> updateNickname(@AuthenticationPrincipal UserDetails userDetails,
                                              @Valid @RequestBody NicknameUpdateRequest request) {
        Long memberId = Long.parseLong(userDetails.getUsername());
        memberService.updateNickname(memberId, request);

        return SuccessResult.of("닉네임이 변경되었습니다.");
    }

    // 비밀번호 변경
    @PatchMapping("/me/password")
    public SuccessResult<Void> updatePassword(@AuthenticationPrincipal UserDetails userDetails,
                                              @Valid @RequestBody PasswordUpdateRequest request) {
        Long memberId = Long.parseLong(userDetails.getUsername());
        memberService.updatePassword(memberId, request);

        return SuccessResult.of("비밀번호가 변경되었습니다.");
    }

    // home 등록/변경
    @PatchMapping("/me/home")
    public SuccessResult<HomeUpdateResponse> updateHome(@AuthenticationPrincipal UserDetails userDetails,
                                                        @Valid @RequestBody HomeUpdateRequest request) {
        Long memberId = Long.parseLong(userDetails.getUsername());

        HomeUpdateResponse response = memberService.updateHome(memberId, request);

        return SuccessResult.of("귀가지 설정이 완료되었습니다.", response);
    }

    // home 삭제
    @DeleteMapping("/me/home")
    public SuccessResult<Void> deleteHome(@AuthenticationPrincipal UserDetails userDetails) {
        Long memberId = Long.parseLong(userDetails.getUsername());

        memberService.deleteHome(memberId);

        return SuccessResult.of("귀가지가 삭제되었습니다.");
    }


    // 내 프로필 조회
    @GetMapping("/me")
    public SuccessResult<MyProfileResponse> getMyProfile(@AuthenticationPrincipal UserDetails userDetails) {
        Long memberId = Long.parseLong(userDetails.getUsername());

        MyProfileResponse response = memberService.getMyProfile(memberId);

        return SuccessResult.of("내 프로필 조회", response);
    }

    // 미완성 -----------------------------------------------------------------------------------
    // 회원탈퇴
    // 현재는 껍데기 — 로그만 찍고 성공 응답 반환.
    // 추후 연관 테이블 확정 후 Hard Delete 로직 채울 예정.
    @DeleteMapping("/me")
    public SuccessResult<Void> withdraw(@AuthenticationPrincipal UserDetails userDetails) {
        // 1. 로그를 통해 누가 탈퇴 요청을 했는지 기
        log.info("회원 탈퇴 요청 - memberId: {}", userDetails.getUsername());

        // 2. TODO: 추후에 연관 데이터 수동 삭제 로직(Hard Delete)이 들어갈 자리입니다.


        // 3. 지금은 껍데기이므로 성공 메시지만 반환
        return SuccessResult.of("회원 탈퇴가 정상적으로 처리되었습니다.");
    }
}
