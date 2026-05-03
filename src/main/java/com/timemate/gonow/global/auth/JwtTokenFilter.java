package com.timemate.gonow.global.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenFilter extends OncePerRequestFilter {
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) throws ServletException, IOException {

        // 1. 사용자의 HTTP 요청 헤더에서 "Bearer "를 떼어내고 순수 토큰만 가져옵니다.
        String token = resolveToken(request);

        try {
            // 2. 토큰이 존재한다면, 전문가(Provider)에게 넘겨서 시큐리티 신분증(Authentication)을 받아옵니다.
            if (token != null) {
                // 파싱하다가 이상한 토큰을 발견하면 이 줄에서 Exception을 던지고 아래 코드는 무시됩니다.
                Authentication authentication = jwtTokenProvider.getAuthentication(token);

                // 3. 정상적으로 신분증을 받았다면, 스프링 시큐리티 시스템에 인증 도장을 쾅 찍어줍니다.
                SecurityContextHolder.getContextHolderStrategy().getContext().setAuthentication(authentication);
            }
        } catch (Exception e) {
            // 4. 해커의 조작된 토큰이거나 만료된 토큰인 경우, 서버가 멈추지 않게 조용히 로그를 남깁니다.
            log.error("JWT Token Validation Error : {}", e.getMessage());

            // 5. 프론트엔드(CSR)가 에러를 알아듣고 대처할 수 있도록, 직접 JSON 에러를 만들어서 쏴줍니다.
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("invalid jwtToken");

            return; // 🌟 제일 중요! 에러를 뱉었으니 여기서 검문소를 아예 닫아버리고 로직을 끝냅니다.
        }

        // 6. 비회원용 API라서 토큰이 없거나, 정상적으로 토큰 인증을 마친 경우 다음 필터로 무사히 보내줍니다.
        filterChain.doFilter(request, response);
    }

    // 요청 헤더에서 순수 토큰 값만 쏙 빼내는 유틸 메서드
    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");

        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null; // 토큰이 없거나 형식이 틀리면 null 반환
    }
}