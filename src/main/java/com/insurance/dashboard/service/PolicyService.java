package com.insurance.dashboard.service;

import com.insurance.dashboard.api.dto.request.FlagPoliciesRequest;
import com.insurance.dashboard.api.dto.response.FlagPoliciesResponse;
import com.insurance.dashboard.api.dto.response.PolicySummaryResponse;
import com.insurance.dashboard.api.dto.response.PolicySummaryStats;
import com.insurance.dashboard.domain.model.Policy.LineOfBusiness;
import com.insurance.dashboard.domain.model.Policy.PolicyStatus;
import com.insurance.dashboard.domain.model.Policy.Region;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.UUID;

public interface PolicyService {

    Page<PolicySummaryResponse> getPolicies(PolicyStatus status,
                                            Region region,
                                            LineOfBusiness lineOfBusiness,
                                            LocalDate effectiveDateFrom,
                                            LocalDate effectiveDateTo,
                                            String search,
                                            Pageable pageable);

    PolicySummaryResponse getPolicyById(UUID id);

    FlagPoliciesResponse flagPoliciesForReview(FlagPoliciesRequest request);

    PolicySummaryStats getSummaryStats();
}
