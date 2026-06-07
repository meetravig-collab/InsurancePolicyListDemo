package com.insurance.dashboard.service;

import com.insurance.dashboard.api.dto.response.PolicySummaryResponse;
import com.insurance.dashboard.api.mapper.PolicyMapper;
import com.insurance.dashboard.domain.model.Policy.PolicyStatus;
import com.insurance.dashboard.domain.model.Policy.Region;
import com.insurance.dashboard.infrastructure.persistence.repository.PolicyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PolicyServiceImpl implements PolicyService {

    private final PolicyRepository policyRepository;
    private final PolicyMapper policyMapper;

    @Override
    public Page<PolicySummaryResponse> getPaginatedPolicies(PolicyStatus status, Region region, Pageable pageable) {
        log.debug("Fetching policies - status={}, region={}, page={}", status, region, pageable.getPageNumber());
        return policyRepository.findAllWithFilters(status, region, pageable)
                .map(policyMapper::toResponse);
    }
}
