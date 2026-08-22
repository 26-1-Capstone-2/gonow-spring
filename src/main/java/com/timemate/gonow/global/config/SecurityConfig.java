package com.timemate.gonow.global.config;

import com.timemate.gonow.global.auth.JwtTokenFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtTokenFilter jwtTokenFilter;

    @Bean
    public PasswordEncoder makePassword() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain myFilter(HttpSecurity httpsecurity) {
        return httpsecurity
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .csrf(AbstractHttpConfigurer::disable) // csrf 비활성화
                // 안드로이드나 iOS 네이티브 앱 은 브라우저의 '동일 출처 정책(CORS)' 제약을 받지 않음
                .cors(AbstractHttpConfigurer::disable)
                // 세션을 사용하지 않으므로 상태를 유지하지 않겠다고 설정
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/health",                      // 헬스체크 (GET)
                                "/docs",                        // Swagger UI 진입
                                "/docs/**",                     // Swagger UI 리소스
                                "/swagger-ui/**",               // Swagger UI 리다이렉트 경로
                                "/v3/api-docs/**",              // OpenAPI 스펙 JSON
                                "/api/auth/login",              // 로그인 (POST)
                                "/api/members/check",           // 이메일/닉네임 중복 확인 (GET)
                                "/error",                       // 에러 핸들링 ANY
                                "/.well-known/**",              // 안드로이드 App Links 검증용 assetlinks.json (정적 리소스)
                                "/join",                        // 카톡 등 인앱 브라우저용 intent:// 리다이렉트 페이지 (InviteRedirectController)
                                "/join.html",                   // 위 forward 대상 — 시큐리티가 내부 forward도 재검사하므로 함께 permitAll 필요
                                "/images/**",                    // join.html OG 태그용 로고 등 공개 정적 이미지
                                "/internal/scheduler/ready").permitAll() // TODO: 테스트 완료 후 삭제
                        .requestMatchers(HttpMethod.POST, "/api/members",
                                "/api/members/email-verification",
                                "/api/members/email-verification/confirm").permitAll() // 회원가입 + 이메일 인증코드 발송/확인 (POST, 전부 비로그인 상태 호출)
                        .anyRequest().authenticated() // 그 외 모든 요청은 반드시 우리 서비스의 JWT 토큰이 있어야 함
                )
                // 기존 로그인 필터가 실행되기 전에 우리 JWT 필터를 먼저 실행!
                .addFilterBefore(jwtTokenFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
