package com.timemate.gonow.global.dto;

// Access Token 재발급 응답 — Refresh Token도 매번 새로 발급(회전)한다. 탈취된 Refresh Token은
// 다음 정상 재발급 시점에 자동으로 무효화되어 피해 기간이 "최대 2주"에서 "다음 재발급 전까지"로 줄어들고,
// 활동이 계속되는 한 만료시간도 매번 2주로 밀려나(슬라이딩 세션) 장기 미접속자만 재로그인하게 된다.
public record ReissueResponse(
        String accessToken,
        String refreshToken
) {
    public static ReissueResponse from(String accessToken, String refreshToken) {
        return new ReissueResponse(accessToken, refreshToken);
    }
}
