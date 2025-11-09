package com.hhplus.ecommerce.global.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

public record ErrorResponse(
        String code,
        String message,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        Map<String, Object> details
) {
    public ErrorResponse(String code, String message) {
        this(code, message, null);
    }

    public ErrorResponse(String code, String message, Map<String, Object> details) {
        this.code = code;
        this.message = message;
        this.details = details;
    }

}