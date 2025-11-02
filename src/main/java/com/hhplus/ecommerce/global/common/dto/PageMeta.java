package com.hhplus.ecommerce.global.common.dto;

public record PageMeta(
        int page,
        int size,
        int totalElements,
        int totalPages
) {
}