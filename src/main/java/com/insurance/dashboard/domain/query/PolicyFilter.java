package com.insurance.dashboard.domain.query;

import com.insurance.dashboard.domain.model.Policy.LineOfBusiness;
import com.insurance.dashboard.domain.model.Policy.PolicyStatus;
import com.insurance.dashboard.domain.model.Policy.Region;

import java.time.LocalDate;

/**
 * Domain-owned filter criteria for querying policies.
 * Carries no persistence concerns (no JPA Specification).
 */
public record PolicyFilter(
        PolicyStatus status,
        Region region,
        LineOfBusiness lineOfBusiness,
        LocalDate effectiveDateFrom,
        LocalDate effectiveDateTo,
        String search
) {}
