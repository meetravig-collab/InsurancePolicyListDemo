package com.insurance.dashboard.infrastructure.persistence.entity;

import com.insurance.dashboard.domain.model.Policy;
import org.springframework.stereotype.Component;

/**
 * Maps between the JPA entity and the domain model, keeping the two isolated.
 */
@Component
public class PolicyEntityMapper {

    public Policy toDomain(PolicyEntity e) {
        return Policy.builder()
                .id(e.getId())
                .policyNumber(e.getPolicyNumber())
                .policyholderName(e.getPolicyholderName())
                .lineOfBusiness(e.getLineOfBusiness())
                .status(e.getStatus())
                .premiumAmount(e.getPremiumAmount())
                .currency(e.getCurrency())
                .effectiveDate(e.getEffectiveDate())
                .expiryDate(e.getExpiryDate())
                .region(e.getRegion())
                .underwriter(e.getUnderwriter())
                .flaggedForReview(e.isFlaggedForReview())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }

    public PolicyEntity toEntity(Policy p) {
        return PolicyEntity.builder()
                .id(p.getId())
                .policyNumber(p.getPolicyNumber())
                .policyholderName(p.getPolicyholderName())
                .lineOfBusiness(p.getLineOfBusiness())
                .status(p.getStatus())
                .premiumAmount(p.getPremiumAmount())
                .currency(p.getCurrency())
                .effectiveDate(p.getEffectiveDate())
                .expiryDate(p.getExpiryDate())
                .region(p.getRegion())
                .underwriter(p.getUnderwriter())
                .flaggedForReview(p.isFlaggedForReview())
                .build();
    }
}
