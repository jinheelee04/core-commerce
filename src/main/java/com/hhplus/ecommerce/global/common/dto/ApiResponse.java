package com.hhplus.ecommerce.global.common.dto;

public record ApiResponse<T>(
        T data,
        PageMeta meta
) {
    public static <T> ApiResponse<T> of(T data) {
        return new ApiResponse<>(data, null);
    }

    public static <T> ApiResponse<T> of(T data, PageMeta meta) {
        return new ApiResponse<>(data, meta);
    }

    public static <T> ApiResponse<T> empty() {
        return new ApiResponse<>(null, null);
    }
}