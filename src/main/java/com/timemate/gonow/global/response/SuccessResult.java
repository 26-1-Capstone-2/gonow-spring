package com.timemate.gonow.global.response;

// 성공 응답 공통 포맷
public record SuccessResult<T>(
        String message,
        T data
) {
    public static <T> SuccessResult<T> of(String message, T data) {
        return new SuccessResult<>(message, data);
    }

    public static <T> SuccessResult<T> of(T data) {
        return new SuccessResult<>("요청이 성공적으로 처리되었습니다.", data);
    }

    public static <T> SuccessResult<T> of(String message) {
        return new SuccessResult<>(message, null);
    }
}
