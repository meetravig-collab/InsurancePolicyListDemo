package com.insurance.dashboard.api.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class PolicySummaryResponse {

    private UUID id;
    private String policyNumber;
    private String policyholderName;
    private String lineOfBusiness;
    private String status;
    private BigDecimal premiumAmount;
    private String currency;
    private LocalDate effectiveDate;
    private LocalDate expiryDate;
    private String region;
    private String underwriter;
    private boolean flaggedForReview;
    @JsonProperty("isExpiringSoon")
    private boolean isExpiringSoon;
    private Instant createdAt;
    private Instant updatedAt;
}
