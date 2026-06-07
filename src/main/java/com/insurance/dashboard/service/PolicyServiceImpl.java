package com.insurance.dashboard.service;

import com.insurance.dashboard.common.exception.PolicyNotFoundException;
import com.insurance.dashboard.domain.model.Policy;
import com.insurance.dashboard.domain.model.Policy.LineOfBusiness;
import com.insurance.dashboard.domain.model.Policy.PolicyStatus;
import com.insurance.dashboard.domain.port.PolicyRepositoryPort;
import com.insurance.dashboard.domain.query.PageQuery;
import com.insurance.dashboard.domain.query.PageResult;
import com.insurance.dashboard.domain.query.PolicyFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@Transactional(readOnly = true)
public class PolicyServiceImpl implements PolicyService {

    private final PolicyRepositoryPort policyRepository;
    private final int expiryWarningDays;

    public PolicyServiceImpl(PolicyRepositoryPort policyRepository,
                             @Value("${policy.expiry.warning-days:30}") int expiryWarningDays) {
        this.policyRepository = policyRepository;
        this.expiryWarningDays = expiryWarningDays;
    }

    @Override
    public PageResult<Policy> getPolicies(PolicyFilter filter, PageQuery page) {
        log.debug("Fetching policies - filter={}, page={}", filter, page.page());
        return policyRepository.findAll(filter, page);
    }

    @Override
    public Policy getPolicyById(UUID id) {
        log.debug("Fetching policy id={}", id);
        return policyRepository.findById(id)
                .orElseThrow(() -> new PolicyNotFoundException(id));
    }

    @Override
    @Transactional
    public int flagPoliciesForReview(List<UUID> policyIds) {
        log.info("Flagging {} policies for review: {}", policyIds.size(), policyIds);
        int flaggedCount = policyRepository.flagForReview(policyIds);
        log.info("Successfully flagged {} policies", flaggedCount);
        return flaggedCount;
    }

    @Override
    public PolicySummary getSummary() {
        log.debug("Fetching policy summary stats");

        Map<String, Long> countsByStatus = new LinkedHashMap<>();
        policyRepository.countByStatus()
                .forEach((status, count) -> countsByStatus.put(toTitleCase(status.name()), count));

        Map<String, BigDecimal> premiumByLob = new LinkedHashMap<>();
        policyRepository.totalPremiumByLineOfBusiness()
                .forEach((lob, total) -> premiumByLob.put(lob.getDisplayName(), total));

        LocalDate today = LocalDate.now();
        long expiringSoon = policyRepository.countExpiringSoon(
                PolicyStatus.ACTIVE, today, today.plusDays(expiryWarningDays));

        return new PolicySummary(countsByStatus, premiumByLob, expiringSoon);
    }

    private static String toTitleCase(String value) {
        if (value == null || value.isEmpty()) return value;
        return value.substring(0, 1).toUpperCase() + value.substring(1).toLowerCase();
    }
}
