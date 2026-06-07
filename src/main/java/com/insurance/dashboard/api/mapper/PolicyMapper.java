package com.insurance.dashboard.api.mapper;

import com.insurance.dashboard.api.dto.response.PolicySummaryResponse;
import com.insurance.dashboard.api.dto.response.PolicySummaryStats;
import com.insurance.dashboard.domain.model.Policy;
import com.insurance.dashboard.service.PolicySummary;

public interface PolicyMapper {

    PolicySummaryResponse toResponse(Policy policy);

    PolicySummaryStats toStats(PolicySummary summary);
}
