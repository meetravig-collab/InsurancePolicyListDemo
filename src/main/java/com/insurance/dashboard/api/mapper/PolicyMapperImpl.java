package com.insurance.dashboard.api.mapper;

import com.insurance.dashboard.api.dto.response.PolicySummaryResponse;
import com.insurance.dashboard.api.dto.response.PolicySummaryResponse.PremiumDto;
import com.insurance.dashboard.domain.model.Policy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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

        String holderName = policy.getPolicyHolder() != null
                ? policy.getPolicyHolder().getFirstName() + " " + policy.getPolicyHolder().getLastName()
                : null;

        if (policy.getStatus() == null) {
            log.warn("Policy id={} has null status", policy.getId());
        }

        return PolicySummaryResponse.builder()
                .id(policy.getId())
                .policyNumber(policy.getPolicyNumber())
                .holderName(holderName)
                .region(policy.getRegion() != null ? policy.getRegion().getDisplayName() : null)
                .status(policy.getStatus() != null ? toTitleCase(policy.getStatus().name()) : null)
                .premium(PremiumDto.builder()
                        .amount(policy.getPremiumAmount())
                        .currency(policy.getCurrency() != null ? policy.getCurrency() : "USD")
                        .build())
                .startDate(policy.getStartDate())
                .endDate(policy.getEndDate())
                .isExpiringSoon(isExpiringSoon(policy.getEndDate()))
                .build();
    }

    private boolean isExpiringSoon(LocalDate endDate) {
        if (endDate == null) return false;
        LocalDate today = LocalDate.now();
        return !endDate.isBefore(today) && endDate.isBefore(today.plusDays(expiryWarningDays));
    }

    private static String toTitleCase(String value) {
        if (value == null || value.isEmpty()) return value;
        return value.substring(0, 1).toUpperCase() + value.substring(1).toLowerCase();
    }
}
