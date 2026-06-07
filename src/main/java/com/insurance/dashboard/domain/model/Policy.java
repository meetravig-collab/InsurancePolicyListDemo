package com.insurance.dashboard.domain.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "policies")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Policy {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank
    @Column(unique = true)
    private String policyNumber;

    private String policyholderName;

    @Enumerated(EnumType.STRING)
    private LineOfBusiness lineOfBusiness;

    @Enumerated(EnumType.STRING)
    private PolicyStatus status;

    @Positive
    private BigDecimal premiumAmount;

    private String currency;

    private LocalDate effectiveDate;

    private LocalDate expiryDate;

    @Enumerated(EnumType.STRING)
    private Region region;

    private String underwriter;

    @Builder.Default
    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean flaggedForReview = false;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
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
