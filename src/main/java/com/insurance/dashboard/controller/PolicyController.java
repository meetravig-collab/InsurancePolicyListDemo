package com.insurance.dashboard.controller;

import com.insurance.dashboard.dto.PolicySummaryResponse;
import com.insurance.dashboard.model.Policy.PolicyStatus;
import com.insurance.dashboard.model.Policy.Region;
import com.insurance.dashboard.service.PolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

}
