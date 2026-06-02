package com.timemate.gonow.global.service;

import com.timemate.gonow.global.dto.LoginRequest;
import com.timemate.gonow.global.dto.LoginResponse;
import com.timemate.gonow.domain.member.entity.Member;
import com.timemate.gonow.domain.member.repository.MemberRepository;
import com.timemate.gonow.global.auth.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AuthService {
    private final MemberRepository memberRepository;

    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    // 로그아웃 — FCM 토큰 null 처리
    @Transactional
    public void logout(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));
        member.updateFcmToken(null);
    }

    // 로그인 후 JWT 토큰 발급
    public LoginResponse login(LoginRequest request) {
        Member member = memberRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalArgumentException("이메일 또는 비밀번호가 일치하지 않습니다."));

        // 좌: 사용자가 입력한 비밀번호(평문)
        // 우: DB에 저장된 비밀번호(암호문)
        if (!passwordEncoder.matches(request.password(), member.getPassword())) {
            throw new IllegalArgumentException("이메일 또는 비밀번호가 일치하지 않습니다.");
        }

        Long id = member.getId();
        String jwtToken = jwtTokenProvider.createToken(id);

        return LoginResponse.from(id, jwtToken);
    }
}
