package com.insurance.dashboard.api.controller;

import com.insurance.dashboard.api.dto.request.FlagPoliciesRequest;
import com.insurance.dashboard.api.dto.response.FlagPoliciesResponse;
import com.insurance.dashboard.api.dto.response.PolicySummaryResponse;
import com.insurance.dashboard.api.dto.response.PolicySummaryStats;
import com.insurance.dashboard.domain.model.Policy.LineOfBusiness;
import com.insurance.dashboard.domain.model.Policy.PolicyStatus;
import com.insurance.dashboard.domain.model.Policy.Region;
import com.insurance.dashboard.service.PolicyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/policies")
@RequiredArgsConstructor
public class PolicyController {

    private final PolicyService policyService;

    @GetMapping
    public ResponseEntity<Page<PolicySummaryResponse>> getPolicies(
            @RequestParam(required = false) PolicyStatus status,
            @RequestParam(required = false) Region region,
            @RequestParam(required = false) LineOfBusiness lineOfBusiness,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate effectiveDateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate effectiveDateTo,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 10, sort = "effectiveDate", direction = Sort.Direction.DESC) Pageable pageable) {
        log.debug("GET /api/v1/policies - status={}, region={}, lob={}, search={}", status, region, lineOfBusiness, search);
        return ResponseEntity.ok(policyService.getPolicies(
                status, region, lineOfBusiness, effectiveDateFrom, effectiveDateTo, search, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PolicySummaryResponse> getPolicyById(@PathVariable UUID id) {
        log.debug("GET /api/v1/policies/{}", id);
        return ResponseEntity.ok(policyService.getPolicyById(id));
    }

    @PatchMapping("/flag")
    public ResponseEntity<FlagPoliciesResponse> flagPoliciesForReview(
            @Valid @RequestBody FlagPoliciesRequest request) {
        log.info("PATCH /api/v1/policies/flag - ids={}", request.getPolicyIds());
        return ResponseEntity.ok(policyService.flagPoliciesForReview(request));
    }

    @GetMapping("/summary")
    public ResponseEntity<PolicySummaryStats> getSummaryStats() {
        log.debug("GET /api/v1/policies/summary");
        return ResponseEntity.ok(policyService.getSummaryStats());
    }
}
