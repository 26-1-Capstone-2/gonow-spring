package com.timemate.gonow.domain.member.service;

import com.timemate.gonow.domain.member.dto.SignupRequest;
import com.timemate.gonow.domain.member.dto.UpdateNicknameRequest;
import com.timemate.gonow.domain.member.dto.UpdatePasswordRequest;
import com.timemate.gonow.domain.member.entity.Member;
import com.timemate.gonow.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MemberService {
    private final MemberRepository memberRepository;
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

        // 3. 모든 검사를 통과하면 비로소 저장!
        Member member = Member.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .nickname(request.nickname())
                .build();

        memberRepository.save(member);

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
    public void updateNickname(Long memberId, UpdateNicknameRequest request) {
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
            throw new IllegalStateException("이미 사용 중인 닉네임입니다.");
        }

        // 4. 닉네임 변경
        member.updateNickname(newNickname);
    }

    // 비밀번호 변경
    @Transactional
    public void updatePassword(Long memberId, UpdatePasswordRequest request) {
        // 1. 회원 조회
        Member member = memberRepository.findById(memberId).orElseThrow(
                () -> new IllegalArgumentException("존재하지 않는 회원입니다.")
        );

        String currentPassword = request.currentPassword();
        String newPassword = request.newPassword();
        String newPasswordConfirm = request.newPasswordConfirm();

        // 2. 현재 비밀번호 일치 여부 확인 (BCryptPasswordEncoder 활용)
        if (!passwordEncoder.matches(currentPassword, member.getPassword())) {
            throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다.");
        }

        // 3. 현재 비밀번호와 새 비밀번호가 같은지 확인
        if (currentPassword.equals(newPassword)) {
            throw new IllegalArgumentException("기존 비밀번호와 다른 비밀번호를 입력해주세요.");
        }

        // 3. 새 비밀번호와 새 비밀번호 확인 일치 여부
        if (!newPassword.equals(newPasswordConfirm)) {
            throw new IllegalArgumentException("새 비밀번호가 서로 일치하지 않습니다.");
        }

        // 4. 새 비밀번호 암호화 후 변경
        String encryptedPassword = passwordEncoder.encode(request.newPassword());
        member.updatePassword(encryptedPassword);
    }
}
