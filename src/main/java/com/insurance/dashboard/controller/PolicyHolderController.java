package com.insurance.dashboard.controller;

import com.insurance.dashboard.model.PolicyHolder;
import com.insurance.dashboard.service.PolicyHolderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/policy-holders")
@RequiredArgsConstructor
public class PolicyHolderController {

    private final PolicyHolderService policyHolderService;

    @GetMapping
    public ResponseEntity<List<PolicyHolder>> getAllPolicyHolders(
            @RequestParam(required = false) String lastName) {
        if (lastName != null) {
            return ResponseEntity.ok(policyHolderService.searchByLastName(lastName));
        }
        return ResponseEntity.ok(policyHolderService.getAllPolicyHolders());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PolicyHolder> getPolicyHolderById(@PathVariable Long id) {
        return ResponseEntity.ok(policyHolderService.getPolicyHolderById(id));
    }

    @PostMapping
    public ResponseEntity<PolicyHolder> createPolicyHolder(@Valid @RequestBody PolicyHolder policyHolder) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(policyHolderService.createPolicyHolder(policyHolder));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PolicyHolder> updatePolicyHolder(
            @PathVariable Long id, @Valid @RequestBody PolicyHolder policyHolder) {
        return ResponseEntity.ok(policyHolderService.updatePolicyHolder(id, policyHolder));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePolicyHolder(@PathVariable Long id) {
        policyHolderService.deletePolicyHolder(id);
        return ResponseEntity.noContent().build();
    }
}
