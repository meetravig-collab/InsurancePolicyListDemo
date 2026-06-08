package com.insurance.dashboard.service;

import com.insurance.dashboard.domain.exception.PolicyNotFoundException;
import com.insurance.dashboard.domain.model.Policy;
import com.insurance.dashboard.domain.model.Policy.LineOfBusiness;
import com.insurance.dashboard.domain.model.Policy.PolicyStatus;
import com.insurance.dashboard.domain.model.Policy.Region;
import com.insurance.dashboard.domain.port.PolicyRepositoryPort;
import com.insurance.dashboard.domain.query.PageQuery;
import com.insurance.dashboard.domain.query.PageResult;
import com.insurance.dashboard.domain.query.PolicyFilter;
import com.insurance.dashboard.domain.query.SortDirection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PolicyServiceTest {

    @Mock private PolicyRepositoryPort policyRepository;

    private PolicyServiceImpl policyService;
    private Policy policy;

    private final PageQuery pageQuery = new PageQuery(0, 10, "effectiveDate", SortDirection.DESC);

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
    void getPolicies_delegatesToPortAndReturnsResult() {
        PolicyFilter filter = new PolicyFilter(null, null, null, null, null, null);
        when(policyRepository.findAll(filter, pageQuery))
                .thenReturn(new PageResult<>(List.of(policy), 1, 0, 10));

        PageResult<Policy> result = policyService.getPolicies(filter, pageQuery);

        assertThat(result.totalElements()).isEqualTo(1);
        assertThat(result.content().get(0).getPolicyNumber()).isEqualTo("POL-100001");
        verify(policyRepository).findAll(filter, pageQuery);
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
        when(policyRepository.flagForReview(ids)).thenReturn(2);

        int count = policyService.flagPoliciesForReview(ids);

        assertThat(count).isEqualTo(2);
        verify(policyRepository).flagForReview(ids);
    }

    @Test
    void getSummary_formatsAggregatesFromPort() {
        when(policyRepository.countByStatus())
                .thenReturn(Map.of(PolicyStatus.ACTIVE, 5L, PolicyStatus.PENDING, 2L));
        when(policyRepository.totalPremiumByLineOfBusiness())
                .thenReturn(Map.of(LineOfBusiness.ACCIDENT_AND_HEALTH, new BigDecimal("850000.00")));
        when(policyRepository.countExpiringSoon(eq(PolicyStatus.ACTIVE), any(), any())).thenReturn(3L);

        PolicySummary summary = policyService.getSummary();

        assertThat(summary.countsByStatus()).containsEntry("Active", 5L).containsEntry("Pending", 2L);
        assertThat(summary.totalPremiumByLineOfBusiness()).containsEntry("A&H", new BigDecimal("850000.00"));
        assertThat(summary.expiringSoonCount()).isEqualTo(3L);
    }
}
