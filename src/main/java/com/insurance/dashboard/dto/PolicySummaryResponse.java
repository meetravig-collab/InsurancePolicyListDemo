package com.insurance.dashboard.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
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
    @JsonProperty("isExpiringSoon")
    private boolean isExpiringSoon;

    @Data
    @Builder
    public static class PremiumDto {
        private BigDecimal amount;
        private String currency;
    }

    private static boolean isExpiringSoon(LocalDate endDate) {
        if (endDate == null) return false;
        LocalDate today = LocalDate.now();
        return !endDate.isBefore(today) && endDate.isBefore(today.plusDays(30));
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
                .isExpiringSoon(isExpiringSoon(policy.getEndDate()))
                .build();
    }
}
