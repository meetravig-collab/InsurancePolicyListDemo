package com.insurance.dashboard.api.mapper;

import com.insurance.dashboard.api.dto.response.PolicySummaryResponse;
import com.insurance.dashboard.api.dto.response.PolicySummaryStats;
import com.insurance.dashboard.domain.model.Policy;
import com.insurance.dashboard.domain.query.PageQuery;
import com.insurance.dashboard.domain.query.SortDirection;
import com.insurance.dashboard.service.PolicySummary;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Slf4j
@Component
public class PolicyMapperImpl implements PolicyMapper {

    private final int expiryWarningDays;

    public PolicyMapperImpl(@Value("${policy.expiry.warning-days:30}") int expiryWarningDays) {
        this.expiryWarningDays = expiryWarningDays;
    }

    @Override
    public PolicySummaryResponse toResponse(Policy policy) {
        log.debug("Mapping policy id={} to response", policy.getId());

        if (policy.getStatus() == null) {
            log.warn("Policy id={} has null status", policy.getId());
        }

        return PolicySummaryResponse.builder()
                .id(policy.getId())
                .policyNumber(policy.getPolicyNumber())
                .policyholderName(policy.getPolicyholderName())
                .lineOfBusiness(policy.getLineOfBusiness() != null ? policy.getLineOfBusiness().getDisplayName() : null)
                .status(policy.getStatus() != null ? toTitleCase(policy.getStatus().name()) : null)
                .premiumAmount(policy.getPremiumAmount())
                .currency(policy.getCurrency())
                .effectiveDate(policy.getEffectiveDate())
                .expiryDate(policy.getExpiryDate())
                .region(policy.getRegion() != null ? policy.getRegion().getDisplayName() : null)
                .underwriter(policy.getUnderwriter())
                .flaggedForReview(policy.isFlaggedForReview())
                .isExpiringSoon(isExpiringSoon(policy.getExpiryDate()))
                .createdAt(policy.getCreatedAt())
                .updatedAt(policy.getUpdatedAt())
                .build();
    }

    @Override
    public PolicySummaryStats toStats(PolicySummary summary) {
        return PolicySummaryStats.builder()
                .countsByStatus(summary.countsByStatus())
                .totalPremiumByLineOfBusiness(summary.totalPremiumByLineOfBusiness())
                .expiringSoonCount(summary.expiringSoonCount())
                .build();
    }

    @Override
    public PageQuery toPageQuery(Pageable pageable) {
        Sort.Order order = pageable.getSort().stream().findFirst().orElse(null);
        String sortField = order != null ? order.getProperty() : "effectiveDate";
        SortDirection direction = (order != null && order.isAscending()) ? SortDirection.ASC : SortDirection.DESC;
        return new PageQuery(pageable.getPageNumber(), pageable.getPageSize(), sortField, direction);
    }

    private boolean isExpiringSoon(LocalDate expiryDate) {
        if (expiryDate == null) return false;
        LocalDate today = LocalDate.now();
        return !expiryDate.isBefore(today) && expiryDate.isBefore(today.plusDays(expiryWarningDays));
    }

    private static String toTitleCase(String value) {
        if (value == null || value.isEmpty()) return value;
        return value.substring(0, 1).toUpperCase() + value.substring(1).toLowerCase();
    }
}
