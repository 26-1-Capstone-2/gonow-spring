package com.timemate.gonow.global.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

// Refresh Token을 Redis에 회원별로 저장/검증/삭제한다. 액세스 토큰(JwtTokenProvider)과 달리
// JWT로 만들지 않고 순수 랜덤 문자열(UUID)을 쓴다 — 이렇게 해야 리프레시 토큰을 액세스 토큰
// 대신 API 호출에 잘못 쓸 수 없다(JwtTokenFilter가 파싱을 시도하면 형식이 달라 바로 거부됨).
@Component
@RequiredArgsConstructor
public class RefreshTokenService {
    private static final String KEY_PREFIX = "refresh:"; // Redis 키 접두사 — 실제 키는 "refresh:{memberId}"

    private final StringRedisTemplate redisTemplate; // Refresh Token 저장/조회/삭제에 쓰는 Redis 클라이언트

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration; // 분 단위 — Refresh Token 만료 기한(2주)

    // 로그인 성공 시 새 Refresh Token 발급 — 기존 값이 있어도 덮어씀(Redis SET이 곧 Upsert,
    // 여러 기기 동시 로그인 시 가장 최근 로그인의 토큰만 유효해짐)
    public String createRefreshToken(Long memberId) {
        String refreshToken = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(key(memberId), refreshToken, Duration.ofMinutes(refreshExpiration));
        return refreshToken;
    }

    // Access Token 재발급(reissue) 요청 시, 회원이 함께 제시한 Refresh Token이
    // Redis에 저장된 값과 일치하는지 확인(발급/재발급 없이 검증만 함)
    public boolean isRefreshTokenValid(Long memberId, String refreshToken) {
        String savedRefreshToken = redisTemplate.opsForValue().get(key(memberId));
        return savedRefreshToken != null && savedRefreshToken.equals(refreshToken);
    }

    // 로그아웃 시 삭제 — 이후 이 Refresh Token을 들고 Access Token 재발급을 요청해도 거부됨
    public void deleteRefreshToken(Long memberId) {
        redisTemplate.delete(key(memberId));
    }

    // 회원별 Redis 키 생성
    private String key(Long memberId) {
        return KEY_PREFIX + memberId;
    }
}
