package com.insurance.dashboard.service;

import com.insurance.dashboard.model.PolicyHolder;
import com.insurance.dashboard.repository.PolicyHolderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PolicyHolderService {

    private final PolicyHolderRepository policyHolderRepository;

    public List<PolicyHolder> getAllPolicyHolders() {
        return policyHolderRepository.findAll();
    }

    public PolicyHolder getPolicyHolderById(Long id) {
        return policyHolderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("PolicyHolder not found with id: " + id));
    }

    public PolicyHolder createPolicyHolder(PolicyHolder policyHolder) {
        return policyHolderRepository.save(policyHolder);
    }

    public PolicyHolder updatePolicyHolder(Long id, PolicyHolder updated) {
        PolicyHolder existing = getPolicyHolderById(id);
        existing.setFirstName(updated.getFirstName());
        existing.setLastName(updated.getLastName());
        existing.setEmail(updated.getEmail());
        existing.setPhone(updated.getPhone());
        existing.setDateOfBirth(updated.getDateOfBirth());
        existing.setAddress(updated.getAddress());
        return policyHolderRepository.save(existing);
    }

    public void deletePolicyHolder(Long id) {
        policyHolderRepository.deleteById(id);
    }

    public List<PolicyHolder> searchByLastName(String lastName) {
        return policyHolderRepository.findByLastNameContainingIgnoreCase(lastName);
    }
}
