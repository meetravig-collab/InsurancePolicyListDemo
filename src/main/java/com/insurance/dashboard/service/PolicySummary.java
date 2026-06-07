package com.insurance.dashboard.service;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Application-layer result of the summary aggregation.
 * Owned by the service layer so the service does not depend on any API/DTO type.
 */
public record PolicySummary(
        Map<String, Long> countsByStatus,
        Map<String, BigDecimal> totalPremiumByLineOfBusiness,
        long expiringSoonCount
) {}
