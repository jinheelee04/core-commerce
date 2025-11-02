package com.hhplus.ecommerce.global.common.dto;

import java.util.Map;

public record ErrorResponse(
        String code,
        String message,
        Map<String, Object> details
) {
    public ErrorResponse(String code, String message) {
        this(code, message, null);
    }
}