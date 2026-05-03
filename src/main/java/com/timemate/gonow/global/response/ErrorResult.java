package com.timemate.gonow.global.response;

// 실패 응답 공통 포맷
public record ErrorResult(
        String message
) {
    public static ErrorResult of(String message) {
        return new ErrorResult(message);
    }
}
