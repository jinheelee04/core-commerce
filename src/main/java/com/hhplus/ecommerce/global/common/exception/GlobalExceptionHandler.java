package com.hhplus.ecommerce.global.common.exception;

import com.hhplus.ecommerce.global.common.dto.ErrorResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

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

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        Map<String, Object> details = new HashMap<>();

        Map<String, String> fieldErrors = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        error -> error.getDefaultMessage() != null ? error.getDefaultMessage() : "Invalid value",
                        (existing, replacement) -> existing
                ));

        details.put("errors", fieldErrors);

        ErrorResponse errorResponse = new ErrorResponse(
                "INVALID_INPUT",
                "입력값이 올바르지 않습니다",
                details
        );

        return ResponseEntity
                .status(400)
                .body(errorResponse);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(ConstraintViolationException e) {
        Map<String, Object> details = new HashMap<>();

        Map<String, String> violations = e.getConstraintViolations()
                .stream()
                .collect(Collectors.toMap(
                        violation -> violation.getPropertyPath().toString(),
                        ConstraintViolation::getMessage,
                        (existing, replacement) -> existing
                ));

        details.put("errors", violations);

        ErrorResponse errorResponse = new ErrorResponse(
                "INVALID_INPUT",
                "입력값이 올바르지 않습니다",
                details
        );

        return ResponseEntity
                .status(400)
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
