package com.hhplus.ecommerce.global.dto;

public record PageMeta(
        int page,
        int size,
        int totalElements,
        int totalPages
) {
    public static PageMeta of(int page, int size, int totalElements) {
        int totalPages = (int) Math.ceil((double) totalElements / size);
        return new PageMeta(page, size, totalElements, totalPages);
    }
}