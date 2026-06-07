package com.insurance.dashboard.domain.query;

/**
 * Domain-owned pagination request — independent of Spring Data Pageable.
 */
public record PageQuery(int page, int size, String sortField, SortDirection direction) {}
