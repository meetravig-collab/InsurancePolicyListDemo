package com.insurance.dashboard.domain.query;

import java.util.List;

/**
 * Domain-owned pagination result — independent of Spring Data Page.
 */
public record PageResult<T>(List<T> content, long totalElements, int page, int size) {

    public int totalPages() {
        return size == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
    }
}
