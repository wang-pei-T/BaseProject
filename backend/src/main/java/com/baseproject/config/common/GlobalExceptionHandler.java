package com.baseproject.config.common;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ApiResponse<Object> handleException(Exception ex) {
        return ApiResponse.failure(50000, ex.getMessage(), RequestIdHolder.get());
    }
}

