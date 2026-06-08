package com.insurance.dashboard.config;

/**
 * Names of the application caches. Constants so they can be referenced from
 * @Cacheable / @CacheEvict annotations.
 */
public final class CacheNames {

    private CacheNames() {}

    /** Paginated, filtered policy listings (key = filter + page query). */
    public static final String POLICY_LISTINGS = "policyListings";

    /** Single policy by id (key = UUID). */
    public static final String POLICY_BY_ID = "policyById";

    /** Aggregated summary statistics (single entry). */
    public static final String POLICY_SUMMARY = "policySummary";
}
