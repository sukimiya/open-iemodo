package com.iemodo.common.response;

import lombok.Getter;

import java.util.List;

/**
 * Paginated response wrapper.
 */
@Getter
public class PageResponse<T> {

    private final List<T> content;
    private final long totalElements;
    private final int page;
    private final int size;
    private final int totalPages;

    private PageResponse(List<T> content, long totalElements, int page, int size) {
        this.content = content;
        this.totalElements = totalElements;
        this.page = page;
        this.size = size;
        this.totalPages = size == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
    }

    public static <T> PageResponse<T> of(List<T> content, long totalElements, int page, int size) {
        return new PageResponse<>(content, totalElements, page, size);
    }
}
