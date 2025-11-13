package com.hhplus.ecommerce.global.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.util.List;

@Schema(description = "페이징된 응답 결과")
public record PagedResult<T>(
        @Schema(description = "데이터 리스트")
        List<T> content,
        @Schema(description = "페이징 메타데이터")
        PageMeta meta
) {
    public static <T> PagedResult<T> of(List<T> content, PageMeta meta) {
        return new PagedResult<>(content, meta);
    }

    public static <T> PagedResult<T> of(List<T> allContent, int page, int size) {
        int totalElements = allContent.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);

        int fromIndex = Math.min(page * size, totalElements);
        int toIndex = Math.min(fromIndex + size, totalElements);
        List<T> pagedContent = allContent.subList(fromIndex, toIndex);

        PageMeta meta = new PageMeta(page, size, totalElements, totalPages);
        return new PagedResult<>(pagedContent, meta);
    }

    /**
     * JPA Page 객체로부터 PagedResult 생성 (DB 페이징)
     */
    public static <T> PagedResult<T> of(List<T> content, int page, int size, long totalElements, int totalPages) {
        PageMeta meta = new PageMeta(page, size, (int) totalElements, totalPages);
        return new PagedResult<>(content, meta);
    }

    /**
     * Spring Data Page를 PagedResult로 변환
     */
    public static <T> PagedResult<T> from(Page<T> page) {
        return new PagedResult<>(
                page.getContent(),
                new PageMeta(
                        page.getNumber(),
                        page.getSize(),
                        (int) page.getTotalElements(),
                        page.getTotalPages()
                )
        );
    }
}
