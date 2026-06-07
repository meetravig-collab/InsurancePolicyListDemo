package com.insurance.dashboard.api.mapper;

import com.insurance.dashboard.api.dto.response.PolicySummaryResponse;
import com.insurance.dashboard.domain.model.Policy;

public interface PolicyMapper {
    PolicySummaryResponse toResponse(Policy policy);
}
