package com.insurance.dashboard.service;

import com.insurance.dashboard.domain.model.Policy.LineOfBusiness;
import com.insurance.dashboard.domain.model.Policy.PolicyStatus;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Application-layer result of the summary aggregation, keyed by domain enums.
 * Display formatting (title-case status, line-of-business display names) is the API
 * layer's concern and is applied by the mapper — not here.
 */
public record PolicySummary(
        Map<PolicyStatus, Long> countsByStatus,
        Map<LineOfBusiness, BigDecimal> totalPremiumByLineOfBusiness,
        long expiringSoonCount
) {}
