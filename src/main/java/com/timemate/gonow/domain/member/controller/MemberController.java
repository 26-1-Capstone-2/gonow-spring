package com.timemate.gonow.domain.member.controller;

import com.timemate.gonow.domain.member.dto.*;
import com.timemate.gonow.domain.member.service.EmailVerificationService;
import com.timemate.gonow.domain.member.service.MemberService;
import com.timemate.gonow.global.response.ApiResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.timemate.gonow.global.auth.MemberId;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/members")
public class MemberController {
    private final MemberService memberService;
    private final EmailVerificationService emailVerificationService;

    // 회원가입
    @PostMapping
    public ApiResult<Void> signUp(@Valid @RequestBody SignupRequest request) {
        memberService.signUp(request);

        return ApiResult.success("회원가입 완료");
    }

    // 이메일 인증코드 발송 (재발송 시 기존 코드 갱신)
    @PostMapping("/email-verification")
    public ApiResult<Void> sendEmailVerification(@Valid @RequestBody EmailVerificationSendRequest request) {
        emailVerificationService.sendCode(request.email());

        return ApiResult.success("인증코드 발송 완료");
    }

    // 이메일 인증코드 확인
    @PostMapping("/email-verification/confirm")
    public ApiResult<Void> confirmEmailVerification(@Valid @RequestBody EmailVerificationConfirmRequest request) {
        emailVerificationService.confirmCode(request.email(), request.code());

        return ApiResult.success("이메일 인증 완료");
    }

    // 비밀번호 찾기(재설정) — 로그인 없이, 이메일 인증코드 확인 완료 후 새 비밀번호로 변경
    @PatchMapping("/password-reset")
    public ApiResult<Void> resetPassword(@Valid @RequestBody PasswordResetRequest request) {
        memberService.resetPassword(request);

        return ApiResult.success("비밀번호 재설정 완료");
    }

    // 이메일 가입 여부 확인 — 항상 200으로 응답하고 exists 값으로만 결과를 전달한다(에러로 취급 안 함).
    // "중복이면 에러"라는 판단은 호출부(프론트 각 화면)가 exists 값을 보고 알아서 한다.
    @GetMapping(value = "/check", params = "email")
    public ApiResult<ExistsResponse> checkEmailExists(@RequestParam @Email(message = "올바른 이메일 형식이 아닙니다.") String email) {
        boolean exists = memberService.existsByEmail(email);

        return ApiResult.success(exists ? "이미 사용 중인 이메일입니다." : "사용 가능한 이메일입니다.", ExistsResponse.from(exists));
    }

    // 닉네임 사용 중 여부 확인 — 위와 동일한 이유로 항상 200 + exists 값만 전달
    @GetMapping(value = "/check", params = "nickname")
    public ApiResult<ExistsResponse> checkNicknameExists(@RequestParam String nickname) {
        boolean exists = memberService.existsByNickname(nickname);

        return ApiResult.success(exists ? "이미 사용 중인 닉네임입니다." : "사용 가능한 닉네임입니다.", ExistsResponse.from(exists));
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
