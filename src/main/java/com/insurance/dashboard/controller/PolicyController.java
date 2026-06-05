package com.insurance.dashboard.controller;

import com.insurance.dashboard.dto.PolicySummaryResponse;
import com.insurance.dashboard.model.Policy;
import com.insurance.dashboard.model.Policy.PolicyStatus;
import com.insurance.dashboard.model.Policy.Region;
import com.insurance.dashboard.service.PolicyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/policies")
@RequiredArgsConstructor
public class PolicyController {

    private final PolicyService policyService;

    @GetMapping
    public ResponseEntity<Page<PolicySummaryResponse>> getAllPolicies(
            @RequestParam(required = false) PolicyStatus status,
            @RequestParam(required = false) Region region,
            @PageableDefault(size = 10, sort = "startDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(policyService.getPaginatedPolicies(status, region, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Policy> getPolicyById(@PathVariable Long id) {
        return ResponseEntity.ok(policyService.getPolicyById(id));
    }

    @GetMapping("/holder/{holderId}")
    public ResponseEntity<List<Policy>> getPoliciesByHolder(@PathVariable Long holderId) {
        return ResponseEntity.ok(policyService.getPoliciesByHolder(holderId));
    }

    @PostMapping("/holder/{holderId}")
    public ResponseEntity<Policy> createPolicy(
            @PathVariable Long holderId, @Valid @RequestBody Policy policy) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(policyService.createPolicy(holderId, policy));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Policy> updatePolicyStatus(
            @PathVariable Long id, @RequestParam PolicyStatus status) {
        return ResponseEntity.ok(policyService.updatePolicyStatus(id, status));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePolicy(@PathVariable Long id) {
        policyService.deletePolicy(id);
        return ResponseEntity.noContent().build();
    }
}
