package com.insurance.dashboard.service;

import com.insurance.dashboard.dto.PolicySummaryResponse;
import com.insurance.dashboard.model.Policy;
import com.insurance.dashboard.model.Policy.PolicyStatus;
import com.insurance.dashboard.model.Policy.Region;
import com.insurance.dashboard.model.PolicyHolder;
import com.insurance.dashboard.repository.PolicyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PolicyServiceTest {

    @Mock
    private PolicyRepository policyRepository;

    @Mock
    private PolicyHolderService policyHolderService;

    @InjectMocks
    private PolicyService policyService;

    private Policy policy;
    private PolicyHolder holder;

    @BeforeEach
    void setUp() {
        holder = PolicyHolder.builder()
                .id(1L).firstName("John").lastName("Smith")
                .email("john@email.com").build();

        policy = Policy.builder()
                .id(1L).policyNumber("POL-001")
                .policyType(Policy.PolicyType.LIFE)
                .premiumAmount(new BigDecimal("250.00"))
                .coverageAmount(new BigDecimal("500000.00"))
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2029, 1, 1))
                .status(PolicyStatus.ACTIVE)
                .region(Region.SINGAPORE)
                .currency("SGD")
                .policyHolder(holder)
                .build();
    }

    @Test
    void getPaginatedPolicies_returnsPageOfSummaries() {
        Pageable pageable = PageRequest.of(0, 10);
        when(policyRepository.findAllWithFilters(null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(policy)));

        Page<PolicySummaryResponse> result = policyService.getPaginatedPolicies(null, null, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getPolicyNumber()).isEqualTo("POL-001");
        assertThat(result.getContent().get(0).getHolderName()).isEqualTo("John Smith");
        assertThat(result.getContent().get(0).getRegion()).isEqualTo("Singapore");
        assertThat(result.getContent().get(0).getStatus()).isEqualTo("Active");
    }

    @Test
    void getPaginatedPolicies_withStatusFilter_delegatesFilterToRepository() {
        Pageable pageable = PageRequest.of(0, 10);
        when(policyRepository.findAllWithFilters(PolicyStatus.ACTIVE, null, pageable))
                .thenReturn(new PageImpl<>(List.of(policy)));

        Page<PolicySummaryResponse> result = policyService.getPaginatedPolicies(PolicyStatus.ACTIVE, null, pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(policyRepository).findAllWithFilters(PolicyStatus.ACTIVE, null, pageable);
    }

    @Test
    void getPaginatedPolicies_withRegionFilter_delegatesFilterToRepository() {
        Pageable pageable = PageRequest.of(0, 10);
        when(policyRepository.findAllWithFilters(null, Region.SINGAPORE, pageable))
                .thenReturn(new PageImpl<>(List.of(policy)));

        Page<PolicySummaryResponse> result = policyService.getPaginatedPolicies(null, Region.SINGAPORE, pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(policyRepository).findAllWithFilters(null, Region.SINGAPORE, pageable);
    }

    @Test
    void getPaginatedPolicies_isExpiringSoon_trueWhenEndDateWithin30Days() {
        policy.setEndDate(LocalDate.now().plusDays(15));
        Pageable pageable = PageRequest.of(0, 10);
        when(policyRepository.findAllWithFilters(null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(policy)));

        Page<PolicySummaryResponse> result = policyService.getPaginatedPolicies(null, null, pageable);

        assertThat(result.getContent().get(0).isExpiringSoon()).isTrue();
    }

    @Test
    void getPaginatedPolicies_isExpiringSoon_falseWhenEndDateBeyond30Days() {
        policy.setEndDate(LocalDate.now().plusDays(60));
        Pageable pageable = PageRequest.of(0, 10);
        when(policyRepository.findAllWithFilters(null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(policy)));

        Page<PolicySummaryResponse> result = policyService.getPaginatedPolicies(null, null, pageable);

        assertThat(result.getContent().get(0).isExpiringSoon()).isFalse();
    }

    @Test
    void getPolicyById_returnsPolicy_whenFound() {
        when(policyRepository.findById(1L)).thenReturn(Optional.of(policy));

        Policy result = policyService.getPolicyById(1L);

        assertThat(result.getPolicyNumber()).isEqualTo("POL-001");
    }

    @Test
    void getPolicyById_throwsException_whenNotFound() {
        when(policyRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> policyService.getPolicyById(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Policy not found with id: 99");
    }

    @Test
    void createPolicy_assignsHolderAndSaves() {
        when(policyHolderService.getPolicyHolderById(1L)).thenReturn(holder);
        when(policyRepository.save(policy)).thenReturn(policy);

        Policy result = policyService.createPolicy(1L, policy);

        assertThat(result.getPolicyHolder()).isEqualTo(holder);
        verify(policyRepository).save(policy);
    }

    @Test
    void updatePolicyStatus_updatesAndSaves() {
        when(policyRepository.findById(1L)).thenReturn(Optional.of(policy));
        when(policyRepository.save(policy)).thenReturn(policy);

        Policy result = policyService.updatePolicyStatus(1L, PolicyStatus.INACTIVE);

        assertThat(result.getStatus()).isEqualTo(PolicyStatus.INACTIVE);
        verify(policyRepository).save(policy);
    }

    @Test
    void deletePolicy_callsRepositoryDelete() {
        doNothing().when(policyRepository).deleteById(1L);

        policyService.deletePolicy(1L);

        verify(policyRepository).deleteById(1L);
    }

    @Test
    void getPoliciesByHolder_returnsListForHolder() {
        when(policyRepository.findByPolicyHolderId(1L)).thenReturn(List.of(policy));

        List<Policy> result = policyService.getPoliciesByHolder(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPolicyNumber()).isEqualTo("POL-001");
    }
}
