package com.hhplus.ecommerce.global.common.exception;

import com.hhplus.ecommerce.global.common.dto.ErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e) {
        ErrorCode errorCode = e.getErrorCode();
        Map<String, Object> details = e.getDetails();

        ErrorResponse errorResponse;
        if (details != null && !details.isEmpty()) {
            errorResponse = new ErrorResponse(
                    errorCode.getCode(),
                    errorCode.getMessage(),
                    details
            );
        } else {
            errorResponse = new ErrorResponse(
                    errorCode.getCode(),
                    errorCode.getMessage()
            );
        }

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        ErrorResponse errorResponse = new ErrorResponse(
                "INTERNAL_SERVER_ERROR",
                "서버 내부 오류가 발생했습니다"
        );

        return ResponseEntity
                .status(500)
                .body(errorResponse);
    }
}
