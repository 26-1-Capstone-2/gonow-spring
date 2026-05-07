package com.timemate.gonow.global.response;

// 공통 응답 포맷
public record ApiResult<T>(
        boolean success,
        String message,
        T data
) {
    // 성공 시: 데이터와 간단한 한글 메시지
    public static <T> ApiResult<T> success(String message, T data) {
        return new ApiResult<>(true, message, data);
    }

    // 데이터만 보내고 싶을 때 (메시지는 null)
    public static <T> ApiResult<T> success(T data) {
        return new ApiResult<>(true, null, data);
    }

    // 메시지만 보내고 싶을 때 (데이터는 null)
    public static <T> ApiResult<T> success(String message) {
        return new ApiResult<>(true, message, null);
    }

    // 실패 시: 짧고 명료한 실패 사유 (한글)
    public static <T> ApiResult<T> fail(String message) {
        return new ApiResult<>(false, message, null);
    }
}