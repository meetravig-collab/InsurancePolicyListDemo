package com.insurance.dashboard.api.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
public class PolicySummaryStats {
    private Map<String, Long> countsByStatus;
    private Map<String, BigDecimal> totalPremiumByLineOfBusiness;
    private long expiringSoonCount;
}
