package com.timemate.gonow.global.exception;

import com.timemate.gonow.global.response.ErrorResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
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
    public ErrorResult handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getAllErrors().getFirst().getDefaultMessage();
        return ErrorResult.of(message);
    }

    // 비즈니스 규칙 위반 (이메일 중복, 비밀번호 불일치 등)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(IllegalArgumentException.class)
    public ErrorResult handleIllegalArgument(IllegalArgumentException e) {
        return ErrorResult.of(e.getMessage());
    }

    // 그 외 예상치 못한 서버 에러
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)
    public ErrorResult handleException(Exception e) {
        log.error("예상치 못한 서버 에러 발생", e);
        return ErrorResult.of("서버 내부 오류가 발생했습니다.");
    }
}
