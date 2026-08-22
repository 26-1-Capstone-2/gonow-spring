package com.timemate.gonow.global.service;

import com.timemate.gonow.global.dto.LoginRequest;
import com.timemate.gonow.global.dto.LoginResponse;
import com.timemate.gonow.global.dto.ReissueRequest;
import com.timemate.gonow.global.dto.ReissueResponse;
import com.timemate.gonow.domain.member.entity.Member;
import com.timemate.gonow.domain.member.repository.MemberRepository;
import com.timemate.gonow.global.auth.JwtTokenProvider;
import com.timemate.gonow.global.auth.RefreshTokenService;
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
    private final RefreshTokenService refreshTokenService;

    // 로그아웃 — FCM 토큰 null 처리 + Refresh Token 폐기(이후 재발급 요청 거부됨)
    @Transactional
    public void logout(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));
        member.updateFcmToken(null);
        refreshTokenService.deleteRefreshToken(memberId); // Refresh Token 폐기
    }

    // 로그인 후 Access/Refresh Token 발급
    public LoginResponse login(LoginRequest request) {
        Member member = memberRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalArgumentException("이메일 또는 비밀번호가 일치하지 않습니다."));

        // 좌: 사용자가 입력한 비밀번호(평문)
        // 우: DB에 저장된 비밀번호(암호문)
        if (!passwordEncoder.matches(request.password(), member.getPassword())) {
            throw new IllegalArgumentException("이메일 또는 비밀번호가 일치하지 않습니다.");
        }

        Long id = member.getId();
        String accessToken = jwtTokenProvider.createAccessToken(id);
        String refreshToken = refreshTokenService.createRefreshToken(id);

        return LoginResponse.from(id, accessToken, refreshToken);
    }

    // Access Token 재발급 — 로그인 없이(Access Token 만료 상태) Refresh Token만으로 호출.
    // 단순 회전(rotation) 방식: 검증 통과 시 Refresh Token도 새로 발급해서 옛 토큰은 즉시 무효화한다.
    // (MySQL을 안 건드리고 Redis만 갱신하므로 login()과 마찬가지로 별도 @Transactional 불필요)
    public ReissueResponse reissue(ReissueRequest request) {
        if (!refreshTokenService.isRefreshTokenValid(request.memberId(), request.refreshToken())) {
            throw new IllegalArgumentException("Refresh Token이 유효하지 않습니다. 다시 로그인해주세요.");
        }

        String accessToken = jwtTokenProvider.createAccessToken(request.memberId());
        String newRefreshToken = refreshTokenService.createRefreshToken(request.memberId());
        return ReissueResponse.from(accessToken, newRefreshToken);
    }
}
