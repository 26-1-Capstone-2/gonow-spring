package com.timemate.gonow.global.exception;

import com.timemate.gonow.global.response.ApiResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    // @Valid 검증 실패 (이메일 형식 오류, 빈 값 등)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResult<Void> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getAllErrors().getFirst().getDefaultMessage();
        return ApiResult.fail(message);
    }

    // 비즈니스 규칙 위반 (이메일 중복, 비밀번호 불일치 등)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(IllegalArgumentException.class)
    public ApiResult<Void> handleIllegalArgument(IllegalArgumentException e) {
        // 내가 의도적으로 던진 에러이므로 메시지를 그대로 전달
        return ApiResult.fail(e.getMessage());
    }

    // 잘못된 Enum 값, JSON 파싱 오류 등 역직렬화 실패
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ApiResult<Void> handleHttpMessageNotReadable(HttpMessageNotReadableException e) {
        log.warn("잘못된 JSON 요청 발생: {}", e.getMessage()); // 개발용 로그
        return ApiResult.fail("요청 데이터 형식이 올바르지 않습니다.");
    }

    // 그 외 예상치 못한 서버 에러
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)
    public ApiResult<Void> handleException(Exception e) {
        log.error("예상치 못한 서버 에러 발생", e); // 서버에만 상세 스택트레이스 기록
        return ApiResult.fail("서버 내부 오류가 발생했습니다.");
    }
}
