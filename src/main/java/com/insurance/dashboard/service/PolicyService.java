package com.insurance.dashboard.service;

import com.insurance.dashboard.domain.model.Policy;
import com.insurance.dashboard.domain.query.PageQuery;
import com.insurance.dashboard.domain.query.PageResult;
import com.insurance.dashboard.domain.query.PolicyFilter;

import java.util.List;
import java.util.UUID;

/**
 * Application service. Depends only on domain types and the domain port —
 * no API/DTO types and no Spring Data / JPA types.
 */
public interface PolicyService {

    PageResult<Policy> getPolicies(PolicyFilter filter, PageQuery page);

    Policy getPolicyById(UUID id);

    int flagPoliciesForReview(List<UUID> policyIds);

    PolicySummary getSummary();
}
