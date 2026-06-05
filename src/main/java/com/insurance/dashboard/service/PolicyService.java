package com.insurance.dashboard.service;

import com.insurance.dashboard.dto.PolicySummaryResponse;
import com.insurance.dashboard.model.Policy;
import com.insurance.dashboard.model.Policy.PolicyStatus;
import com.insurance.dashboard.model.Policy.Region;
import com.insurance.dashboard.model.PolicyHolder;
import com.insurance.dashboard.repository.PolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PolicyService {

    private final PolicyRepository policyRepository;
    private final PolicyHolderService policyHolderService;

    public Page<PolicySummaryResponse> getPaginatedPolicies(PolicyStatus status, Region region, Pageable pageable) {
        return policyRepository.findAllWithFilters(status, region, pageable)
                .map(PolicySummaryResponse::from);
    }

    public List<Policy> getAllPolicies() {
        return policyRepository.findAll();
    }

    public Policy getPolicyById(Long id) {
        return policyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Policy not found with id: " + id));
    }

    public List<Policy> getPoliciesByHolder(Long holderId) {
        return policyRepository.findByPolicyHolderId(holderId);
    }

    public Policy createPolicy(Long holderId, Policy policy) {
        PolicyHolder holder = policyHolderService.getPolicyHolderById(holderId);
        policy.setPolicyHolder(holder);
        return policyRepository.save(policy);
    }

    public Policy updatePolicyStatus(Long id, PolicyStatus status) {
        Policy policy = getPolicyById(id);
        policy.setStatus(status);
        return policyRepository.save(policy);
    }

    public void deletePolicy(Long id) {
        policyRepository.deleteById(id);
    }

    public List<Policy> getPoliciesByStatus(PolicyStatus status) {
        return policyRepository.findByStatus(status);
    }
}
