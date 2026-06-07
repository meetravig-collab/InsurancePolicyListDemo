package com.insurance.dashboard.infrastructure.persistence.entity;

import com.insurance.dashboard.domain.model.Policy;
import jakarta.persistence.*;
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

/**
 * JPA persistence entity. Lives in infrastructure so persistence concerns
 * never leak into the domain model. Reuses the domain enums.
 */
@Entity
@Table(name = "policies")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PolicyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true)
    private String policyNumber;

    private String policyholderName;

    @Enumerated(EnumType.STRING)
    private Policy.LineOfBusiness lineOfBusiness;

    @Enumerated(EnumType.STRING)
    private Policy.PolicyStatus status;

    private BigDecimal premiumAmount;

    private String currency;

    private LocalDate effectiveDate;

    private LocalDate expiryDate;

    @Enumerated(EnumType.STRING)
    private Policy.Region region;

    private String underwriter;

    @Builder.Default
    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean flaggedForReview = false;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;
}
