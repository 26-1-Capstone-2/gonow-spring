package com.timemate.gonow.domain.member.service;

import com.timemate.gonow.domain.common.Location;
import com.timemate.gonow.domain.common.Point;
import com.timemate.gonow.domain.member.dto.*;
import com.timemate.gonow.domain.member.entity.Member;
import com.timemate.gonow.domain.member.entity.MemberSetting;
import com.timemate.gonow.domain.member.repository.MemberRepository;
import com.timemate.gonow.domain.member.repository.MemberSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MemberService {
    private final MemberRepository memberRepository;
    private final MemberSettingRepository memberSettingRepository;

    private final PasswordEncoder passwordEncoder;

    // 회원가입
    // 비밀번호를 암호화해서 DB에 저장한다.
    @Transactional
    public Long signUp(SignupRequest request) {
        // 1. 이메일 중복 체크 (신분증 확인 1)
        memberRepository.findByEmail(request.email())
                .ifPresent(m -> {
                    throw new IllegalArgumentException("이미 가입된 이메일입니다.");
                });

        // 2. 닉네임 중복 체크 (신분증 확인 2)
        memberRepository.findByNickname(request.nickname())
                .ifPresent(m -> {
                    throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
                });

        // 3. 모든 검사를 통과하면 저장
        Member member = Member.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .nickname(request.nickname())
                .build();

        memberRepository.save(member);

        // 회원가입 시 MemberSetting도 함께 저장
        MemberSetting setting = MemberSetting.builder()
                .member(member)
                .build();
        memberSettingRepository.save(setting);

        return member.getId();
    }

    // 이메일 중복 확인 (true = 사용 가능, false = 이미 사용 중)
    public boolean isEmailAvailable(String email) {
        return !memberRepository.existsByEmail(email);
    }

    // 닉네임 중복 확인 (true = 사용 가능, false = 이미 사용 중)
    public boolean isNicknameAvailable(String nickname) {
        return !memberRepository.existsByNickname(nickname);
    }

    // 닉네임 변경
    @Transactional
    public void updateNickname(Long memberId, NicknameUpdateRequest request) {
        // 1. 회원 조회
        Member member = memberRepository.findById(memberId).orElseThrow(
                () -> new IllegalArgumentException("존재하지 않는 회원입니다.")
        );

        String newNickname = request.nickname();

        // 2. 나 자신과 동일한 닉네임이면 통과
        // (사용자에게 "이미 사용 중"이라고 에러를 띄우면, 본인인데도 못 쓴다고 오해할 수 있어 조용히 '성공' 처리합니다.)
        if (member.getNickname().equals(newNickname)) {
            return;
        }

        // 3. 다른 자신과 동일한 닉네임이면 예외 발생
        if (!isNicknameAvailable(newNickname)) {
            throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
        }

        // 4. 닉네임 변경
        member.updateNickname(newNickname);
    }

    // 비밀번호 변경
    @Transactional
    public void updatePassword(Long memberId, PasswordUpdateRequest request) {
        // 1. 회원 조회
        Member member = memberRepository.findById(memberId).orElseThrow(
                () -> new IllegalArgumentException("존재하지 않는 회원입니다.")
        );

        String currentPassword = request.currentPassword();
        String newPassword = request.newPassword();

        // 2. 현재 비밀번호 일치 여부 확인 (BCryptPasswordEncoder 활용)
        if (!passwordEncoder.matches(currentPassword, member.getPassword())) {
            throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다.");
        }

        // 3. 현재 비밀번호와 새 비밀번호가 같은지 확인
        if (currentPassword.equals(newPassword)) {
            throw new IllegalArgumentException("기존 비밀번호와 다른 비밀번호를 입력해주세요.");
        }

        // 4. 새 비밀번호 암호화 후 변경
        String encryptedPassword = passwordEncoder.encode(newPassword);
        member.updatePassword(encryptedPassword);
    }

    // home 등록/변경
    @Transactional
    public HomeUpdateResponse updateHome(Long memberId, HomeUpdateRequest request) {
        // 1. 회원 조회
        Member member = memberRepository.findById(memberId).orElseThrow(
                () -> new IllegalArgumentException("존재하지 않는 회원입니다.")
        );

        // 2. request -> Location 객체 생성
        Location location = new Location(request.address(), new Point(request.lat(), request.lng()));

        // 3. Home 변경
        member.updateHome(location);

        // 4. 업데이트된 정보 반환
        return HomeUpdateResponse.from(member.getLocation());
    }

    // 내 프로필 조회
    public MyProfileResponse getMyProfile(Long memberId) {
        // 역방향 조회: MemberSetting -> Member
        MemberSetting setting = memberSettingRepository.findWithMemberByMemberId(memberId).orElseThrow(
                () -> new IllegalArgumentException("사용자 정보를 찾을 수 없습니다"));

        return MyProfileResponse.from(setting.getMember(), setting);
    }

    // home 삭제
    @Transactional
    public void deleteHome(Long memberId) {
        // 1. 회원 조회
        Member member = memberRepository.findById(memberId).orElseThrow(
                () -> new IllegalArgumentException("존재하지 않는 회원입니다.")
        );

        // 2. Home 정보 비우기(null)
        member.clearHome();
    }
}
