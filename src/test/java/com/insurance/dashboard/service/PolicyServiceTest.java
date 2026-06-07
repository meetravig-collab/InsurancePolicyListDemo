package com.insurance.dashboard.service;

import com.insurance.dashboard.api.dto.response.PolicySummaryResponse;
import com.insurance.dashboard.api.mapper.PolicyMapperImpl;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
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
        policyService = new PolicyServiceImpl(policyRepository, new PolicyMapperImpl(30), 30);

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
    void getPolicies_returnsPageOfSummaries() {
        Pageable pageable = PageRequest.of(0, 10);
        when(policyRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(policy)));

        Page<PolicySummaryResponse> result = policyService.getPolicies(null, null, null, null, null, null, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getPolicyNumber()).isEqualTo("POL-100001");
        assertThat(result.getContent().get(0).getPolicyholderName()).isEqualTo("John Smith");
        assertThat(result.getContent().get(0).getLineOfBusiness()).isEqualTo("Property");
        assertThat(result.getContent().get(0).getRegion()).isEqualTo("Singapore");
        assertThat(result.getContent().get(0).getStatus()).isEqualTo("Active");
        assertThat(result.getContent().get(0).getUnderwriter()).isEqualTo("Acme Underwriting Co.");
        assertThat(result.getContent().get(0).isFlaggedForReview()).isFalse();
    }

    @Test
    void getPolicies_withStatusFilter_delegatesToRepository() {
        Pageable pageable = PageRequest.of(0, 10);
        when(policyRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(policy)));

        Page<PolicySummaryResponse> result = policyService.getPolicies(PolicyStatus.ACTIVE, null, null, null, null, null, pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(policyRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void getPolicies_withRegionFilter_delegatesToRepository() {
        Pageable pageable = PageRequest.of(0, 10);
        when(policyRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(policy)));

        Page<PolicySummaryResponse> result = policyService.getPolicies(null, Region.THAILAND, null, null, null, null, pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(policyRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void getPolicies_withLineOfBusinessFilter_delegatesToRepository() {
        Pageable pageable = PageRequest.of(0, 10);
        when(policyRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(policy)));

        Page<PolicySummaryResponse> result = policyService.getPolicies(null, null, LineOfBusiness.MARINE, null, null, null, pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(policyRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void getPolicies_isExpiringSoon_trueWhenExpiryWithin30Days() {
        policy.setExpiryDate(LocalDate.now().plusDays(15));
        Pageable pageable = PageRequest.of(0, 10);
        when(policyRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(policy)));

        Page<PolicySummaryResponse> result = policyService.getPolicies(null, null, null, null, null, null, pageable);

        assertThat(result.getContent().get(0).isExpiringSoon()).isTrue();
    }

    @Test
    void getPolicies_isExpiringSoon_falseWhenExpiryBeyond30Days() {
        policy.setExpiryDate(LocalDate.now().plusDays(60));
        Pageable pageable = PageRequest.of(0, 10);
        when(policyRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(policy)));

        Page<PolicySummaryResponse> result = policyService.getPolicies(null, null, null, null, null, null, pageable);

        assertThat(result.getContent().get(0).isExpiringSoon()).isFalse();
    }

    @Test
    void getPolicyById_returnsPolicy_whenFound() {
        UUID id = policy.getId();
        when(policyRepository.findById(id)).thenReturn(java.util.Optional.of(policy));

        PolicySummaryResponse result = policyService.getPolicyById(id);

        assertThat(result.getPolicyNumber()).isEqualTo("POL-100001");
    }

    @Test
    void getPolicyById_throwsNotFound_whenMissing() {
        UUID id = UUID.randomUUID();
        when(policyRepository.findById(id)).thenReturn(java.util.Optional.empty());

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> policyService.getPolicyById(id))
                .isInstanceOf(com.insurance.dashboard.common.exception.PolicyNotFoundException.class);
    }
}
