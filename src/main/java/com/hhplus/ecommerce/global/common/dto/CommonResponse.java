package com.hhplus.ecommerce.global.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

public record CommonResponse<T>(
        T data,
        @Schema(hidden = true)
        @JsonInclude(JsonInclude.Include.NON_NULL)
        PageMeta meta
) {
    public static <T> CommonResponse<T> of(T data) {
        return new CommonResponse<>(data, null);
    }

    public static <T> CommonResponse<T> of(T data, PageMeta meta) {
        return new CommonResponse<>(data, meta);
    }

    public static <T> CommonResponse<T> empty() {
        return new CommonResponse<>(null, null);
    }
}
