package com.insurance.dashboard.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Domain model — a persistence-ignorant POJO. It carries no JPA/Hibernate
 * annotations; the JPA mapping lives in infrastructure (PolicyEntity).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Policy {

    private UUID id;
    private String policyNumber;
    private String policyholderName;
    private LineOfBusiness lineOfBusiness;
    private PolicyStatus status;
    private BigDecimal premiumAmount;
    private String currency;
    private LocalDate effectiveDate;
    private LocalDate expiryDate;
    private Region region;
    private String underwriter;
    private boolean flaggedForReview;
    private Instant createdAt;
    private Instant updatedAt;

    public enum LineOfBusiness {
        PROPERTY("Property"),
        CASUALTY("Casualty"),
        ACCIDENT_AND_HEALTH("A&H"),
        MARINE("Marine");

        private final String displayName;

        LineOfBusiness(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public enum PolicyStatus {
        ACTIVE, EXPIRED, PENDING, CANCELLED
    }

    public enum Region {
        SINGAPORE("Singapore"),
        HONG_KONG("Hong Kong"),
        AUSTRALIA("Australia"),
        JAPAN("Japan"),
        THAILAND("Thailand"),
        INDONESIA("Indonesia"),
        MALAYSIA("Malaysia"),
        PHILIPPINES("Philippines");

        private final String displayName;

        Region(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}
