package com.insurance.dashboard.api.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
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
}
