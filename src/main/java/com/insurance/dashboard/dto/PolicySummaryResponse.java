package com.insurance.dashboard.dto;

import com.insurance.dashboard.model.Policy;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class PolicySummaryResponse {

    private Long id;
    private String policyNumber;
    private String holderName;
    private String region;
    private String status;
    private PremiumDto premium;
    private LocalDate startDate;
    private LocalDate endDate;

    @Data
    @Builder
    public static class PremiumDto {
        private BigDecimal amount;
        private String currency;
    }

    private static String toTitleCase(String value) {
        return value.substring(0, 1).toUpperCase() + value.substring(1).toLowerCase();
    }

    public static PolicySummaryResponse from(Policy policy) {
        String holderName = policy.getPolicyHolder() != null
                ? policy.getPolicyHolder().getFirstName() + " " + policy.getPolicyHolder().getLastName()
                : null;

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
                .build();
    }
}
