package com.insurance.dashboard.service;

import com.insurance.dashboard.domain.model.Policy;
import com.insurance.dashboard.domain.model.Policy.LineOfBusiness;
import com.insurance.dashboard.domain.model.Policy.PolicyStatus;
import com.insurance.dashboard.domain.model.Policy.Region;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Application service. Deals only in domain types and application-owned results
 * ({@link Policy}, {@link PolicySummary}) — it has no dependency on the API/DTO layer.
 */
public interface PolicyService {

    Page<Policy> getPolicies(PolicyStatus status,
                             Region region,
                             LineOfBusiness lineOfBusiness,
                             LocalDate effectiveDateFrom,
                             LocalDate effectiveDateTo,
                             String search,
                             Pageable pageable);

    Policy getPolicyById(UUID id);

    int flagPoliciesForReview(List<UUID> policyIds);

    PolicySummary getSummary();
}
