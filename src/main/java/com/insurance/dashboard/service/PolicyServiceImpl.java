package com.insurance.dashboard.service;

import com.insurance.dashboard.common.exception.PolicyNotFoundException;
import com.insurance.dashboard.domain.model.Policy;
import com.insurance.dashboard.domain.model.Policy.LineOfBusiness;
import com.insurance.dashboard.domain.model.Policy.PolicyStatus;
import com.insurance.dashboard.domain.model.Policy.Region;
import com.insurance.dashboard.infrastructure.persistence.PolicySpecification;
import com.insurance.dashboard.infrastructure.persistence.repository.PolicyRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
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

    private final PolicyRepository policyRepository;
    private final int expiryWarningDays;

    public PolicyServiceImpl(PolicyRepository policyRepository,
                             @Value("${policy.expiry.warning-days:30}") int expiryWarningDays) {
        this.policyRepository = policyRepository;
        this.expiryWarningDays = expiryWarningDays;
    }

    @Override
    public Page<Policy> getPolicies(PolicyStatus status, Region region,
                                    LineOfBusiness lineOfBusiness,
                                    LocalDate effectiveDateFrom, LocalDate effectiveDateTo,
                                    String search, Pageable pageable) {
        log.debug("Fetching policies - status={}, region={}, lob={}, search={}, page={}",
                status, region, lineOfBusiness, search, pageable.getPageNumber());
        Specification<Policy> spec = PolicySpecification.withFilters(
                status, region, lineOfBusiness, effectiveDateFrom, effectiveDateTo, search);
        return policyRepository.findAll(spec, pageable);
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
        int flaggedCount = policyRepository.bulkFlagForReview(policyIds);
        log.info("Successfully flagged {} policies", flaggedCount);
        return flaggedCount;
    }

    @Override
    public PolicySummary getSummary() {
        log.debug("Fetching policy summary stats");

        Map<String, Long> countsByStatus = new LinkedHashMap<>();
        policyRepository.countGroupByStatus()
                .forEach(row -> {
                    PolicyStatus s = (PolicyStatus) row[0];
                    countsByStatus.put(toTitleCase(s.name()), (Long) row[1]);
                });

        Map<String, BigDecimal> premiumByLob = new LinkedHashMap<>();
        policyRepository.sumPremiumGroupByLineOfBusiness()
                .forEach(row -> {
                    LineOfBusiness lob = (LineOfBusiness) row[0];
                    premiumByLob.put(lob.getDisplayName(), (BigDecimal) row[1]);
                });

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
