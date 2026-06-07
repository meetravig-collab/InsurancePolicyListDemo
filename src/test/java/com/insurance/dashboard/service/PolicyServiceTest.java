package com.insurance.dashboard.service;

import com.insurance.dashboard.common.exception.PolicyNotFoundException;
import com.insurance.dashboard.domain.model.Policy;
import com.insurance.dashboard.domain.model.Policy.LineOfBusiness;
import com.insurance.dashboard.domain.model.Policy.PolicyStatus;
import com.insurance.dashboard.domain.model.Policy.Region;
import com.insurance.dashboard.infrastructure.persistence.repository.PolicyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PolicyServiceTest {

    @Mock private PolicyRepository policyRepository;

    private PolicyServiceImpl policyService;
    private Policy policy;

    @BeforeEach
    void setUp() {
        policyService = new PolicyServiceImpl(policyRepository, 30);

        policy = Policy.builder()
                .id(UUID.randomUUID()).policyNumber("POL-100001")
                .policyholderName("John Smith")
                .lineOfBusiness(LineOfBusiness.PROPERTY)
                .premiumAmount(new BigDecimal("250000.00"))
                .effectiveDate(LocalDate.of(2024, 1, 1))
                .expiryDate(LocalDate.of(2029, 1, 1))
                .status(PolicyStatus.ACTIVE)
                .region(Region.SINGAPORE)
                .currency("SGD")
                .underwriter("Acme Underwriting Co.")
                .flaggedForReview(false)
                .build();
    }

    @Test
    void getPolicies_returnsDomainPage() {
        Pageable pageable = PageRequest.of(0, 10);
        when(policyRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(policy)));

        Page<Policy> result = policyService.getPolicies(null, null, null, null, null, null, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getPolicyNumber()).isEqualTo("POL-100001");
        assertThat(result.getContent().get(0).getStatus()).isEqualTo(PolicyStatus.ACTIVE);
    }

    @Test
    void getPolicies_appliesFiltersViaSpecification() {
        Pageable pageable = PageRequest.of(0, 10);
        when(policyRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(policy)));

        policyService.getPolicies(PolicyStatus.ACTIVE, Region.JAPAN, LineOfBusiness.MARINE, null, null, "x", pageable);

        verify(policyRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void getPolicyById_returnsPolicy_whenFound() {
        UUID id = policy.getId();
        when(policyRepository.findById(id)).thenReturn(Optional.of(policy));

        Policy result = policyService.getPolicyById(id);

        assertThat(result.getPolicyNumber()).isEqualTo("POL-100001");
    }

    @Test
    void getPolicyById_throwsNotFound_whenMissing() {
        UUID id = UUID.randomUUID();
        when(policyRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> policyService.getPolicyById(id))
                .isInstanceOf(PolicyNotFoundException.class);
    }

    @Test
    void flagPoliciesForReview_returnsUpdatedCount() {
        List<UUID> ids = List.of(UUID.randomUUID(), UUID.randomUUID());
        when(policyRepository.bulkFlagForReview(ids)).thenReturn(2);

        int count = policyService.flagPoliciesForReview(ids);

        assertThat(count).isEqualTo(2);
        verify(policyRepository).bulkFlagForReview(ids);
    }

    @Test
    void getSummary_aggregatesStatusCountsAndPremiums() {
        List<Object[]> statusRows = new java.util.ArrayList<>();
        statusRows.add(new Object[]{PolicyStatus.ACTIVE, 5L});
        statusRows.add(new Object[]{PolicyStatus.PENDING, 2L});
        List<Object[]> premiumRows = new java.util.ArrayList<>();
        premiumRows.add(new Object[]{LineOfBusiness.PROPERTY, new BigDecimal("850000.00")});

        when(policyRepository.countGroupByStatus()).thenReturn(statusRows);
        when(policyRepository.sumPremiumGroupByLineOfBusiness()).thenReturn(premiumRows);
        when(policyRepository.countExpiringSoon(eq(PolicyStatus.ACTIVE), any(), any())).thenReturn(3L);

        PolicySummary summary = policyService.getSummary();

        assertThat(summary.countsByStatus()).containsEntry("Active", 5L).containsEntry("Pending", 2L);
        assertThat(summary.totalPremiumByLineOfBusiness()).containsEntry("Property", new BigDecimal("850000.00"));
        assertThat(summary.expiringSoonCount()).isEqualTo(3L);
    }
}
