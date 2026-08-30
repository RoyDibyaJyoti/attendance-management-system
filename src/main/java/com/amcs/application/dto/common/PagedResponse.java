package com.amcs.application.dto.common;

import java.util.List;

public record PagedResponse<T>(
    List<T> content,
    int page,
    int size,
    long totalElements,
    int totalPages,
    boolean first,
    boolean last
) {
    public static <T> PagedResponse<T> of(List<T> allItems, int page, int size) {
        int fromIndex = Math.min(page * size, allItems.size());
        int toIndex = Math.min(fromIndex + size, allItems.size());
        List<T> subList = allItems.subList(fromIndex, toIndex);
        int totalPages = (int) Math.ceil((double) allItems.size() / size);
        return new PagedResponse<>(
            subList,
            page,
            size,
            allItems.size(),
            totalPages,
            page == 0,
            page >= totalPages - 1
        );
    }
}
