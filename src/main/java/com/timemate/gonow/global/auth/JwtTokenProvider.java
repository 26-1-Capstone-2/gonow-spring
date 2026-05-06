package com.timemate.gonow.global.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.util.Base64;
import java.util.Date;

@Component
public class JwtTokenProvider {
    private final SecretKey SECRET_KEY;
    private final long expiration;

    // [생성자] 서버가 켜질 때, application.yml에 있는 비밀키와 만료시간을 가져와서 세팅합니다.
    public JwtTokenProvider(@Value("${jwt.secret}") String secret, @Value("${jwt.expiration}") long expiration) {
        byte[] keyBytes = Base64.getDecoder().decode(secret);
        this.SECRET_KEY = Keys.hmacShaKeyFor(keyBytes); // 서명에 사용할 수 있는 튼튼한 암호화 키로 변환
        this.expiration = expiration;
    }

    // [1] 토큰 생성기: 로그인에 성공한 유저에게 새 출입증(JWT)을 만들어 줍니다.
    public String createToken(Long id) {
        // 유효 기간 계산 (현재 시간 + 설정된 N분)
        long nowTime = System.currentTimeMillis();
        Date now = new Date(nowTime);

        long expirationMs  = Duration.ofMinutes(expiration).toMillis();
        Date validity = new Date(nowTime + expirationMs);

        return Jwts.builder()
                .subject(String.valueOf(id))  // 토큰 주인의 이름(PK 등의 식별자)
                                              // role은 안 쓸거므로 과감하게 지움
                .issuedAt(now)                // 언제 만들어졌는지
                .expiration(validity)         // 언제까지 유효한지
                .signWith(SECRET_KEY)         // 위조 방지를 위해 우리 서버만의 비밀키로 도장 쾅!
                .compact();                   // 이 모든 걸 압축해서 긴 문자열(토큰)로 조립 및 반환
    }

    // [2] 토큰 해독기: 문지기가 넘겨준 토큰을 까보고, 정상이면 시큐리티 전용 신분증을 만들어 줍니다.
    public Authentication getAuthentication(String token) {
        // 1. 도장을 대조(verifyWith)해서 진짜 토큰이 맞는지 확인하고, 알맹이(Payload)를 꺼냅니다.
        // (만료되었거나 조작된 토큰이면 여기서 가차 없이 Exception 이 터집니다!)
        Claims claims = Jwts.parser()
                .verifyWith(SECRET_KEY)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        // 2. 알맹이에서 유저 정보(id, 권한) 추출
        String id = claims.getSubject();

        // 3. 스프링 시큐리티가 알아들을 수 있는 공식 신분증(UserDetails) 규격으로 포장합니다.
        UserDetails userDetails = User
                .withUsername(id)
                .password("")   // 토큰으로 이미 인증을 마쳤으므로 비밀번호는 형식상 빈 칸으로 둡니다.
                                // role은 안 쓸거므로 과감하게 지움
                .build();

        // 4. 최종적으로 시큐리티 컨텍스트에 담을 수 있는 완벽한 인증 객체를 반환합니다.
        return new UsernamePasswordAuthenticationToken(userDetails, token, userDetails.getAuthorities());
    }
}