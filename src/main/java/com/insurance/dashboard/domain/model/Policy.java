package com.insurance.dashboard.domain.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "policies")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Policy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String policyNumber;

    @Enumerated(EnumType.STRING)
    private PolicyType policyType;

    @Positive
    private BigDecimal premiumAmount;

    @Positive
    private BigDecimal coverageAmount;

    private LocalDate startDate;

    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    private PolicyStatus status;

    @Enumerated(EnumType.STRING)
    private Region region;

    private String currency;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "policy_holder_id")
    private PolicyHolder policyHolder;

    public enum PolicyType {
        LIFE, HEALTH, AUTO, HOME, TRAVEL
    }

    public enum PolicyStatus {
        ACTIVE, INACTIVE, EXPIRED, PENDING, LAPSED
    }

    public enum Region {
        SINGAPORE("Singapore"),
        HONG_KONG("Hong Kong"),
        AUSTRALIA("Australia"),
        INDIA("India"),
        JAPAN("Japan");

        private final String displayName;

        Region(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}
