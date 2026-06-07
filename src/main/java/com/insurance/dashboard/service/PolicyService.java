package com.insurance.dashboard.service;

import com.insurance.dashboard.api.dto.response.PolicySummaryResponse;
import com.insurance.dashboard.domain.model.Policy.PolicyStatus;
import com.insurance.dashboard.domain.model.Policy.Region;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PolicyService {
    Page<PolicySummaryResponse> getPaginatedPolicies(PolicyStatus status, Region region, Pageable pageable);
}
