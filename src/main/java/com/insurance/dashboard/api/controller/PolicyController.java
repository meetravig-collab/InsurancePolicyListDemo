package com.insurance.dashboard.api.controller;

import com.insurance.dashboard.api.dto.request.FlagPoliciesRequest;
import com.insurance.dashboard.api.dto.response.FlagPoliciesResponse;
import com.insurance.dashboard.api.dto.response.PolicySummaryResponse;
import com.insurance.dashboard.api.dto.response.PolicySummaryStats;
import com.insurance.dashboard.api.mapper.PolicyMapper;
import com.insurance.dashboard.domain.model.Policy;
import com.insurance.dashboard.domain.model.Policy.LineOfBusiness;
import com.insurance.dashboard.domain.model.Policy.PolicyStatus;
import com.insurance.dashboard.domain.model.Policy.Region;
import com.insurance.dashboard.domain.query.PageResult;
import com.insurance.dashboard.domain.query.PolicyFilter;
import com.insurance.dashboard.service.PolicyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/policies")
@RequiredArgsConstructor
public class PolicyController {

    private final PolicyService policyService;
    private final PolicyMapper policyMapper;

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

        PolicyFilter filter = new PolicyFilter(status, region, lineOfBusiness, effectiveDateFrom, effectiveDateTo, search);
        PageResult<Policy> result = policyService.getPolicies(filter, policyMapper.toPageQuery(pageable));

        List<PolicySummaryResponse> content = result.content().stream().map(policyMapper::toResponse).toList();
        return ResponseEntity.ok(new PageImpl<>(content, pageable, result.totalElements()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PolicySummaryResponse> getPolicyById(@PathVariable UUID id) {
        log.debug("GET /api/v1/policies/{}", id);
        return ResponseEntity.ok(policyMapper.toResponse(policyService.getPolicyById(id)));
    }

    @PatchMapping("/flag")
    public ResponseEntity<FlagPoliciesResponse> flagPoliciesForReview(
            @Valid @RequestBody FlagPoliciesRequest request) {
        log.info("PATCH /api/v1/policies/flag - ids={}", request.getPolicyIds());
        int flaggedCount = policyService.flagPoliciesForReview(request.getPolicyIds());
        return ResponseEntity.ok(new FlagPoliciesResponse(flaggedCount, request.getPolicyIds()));
    }

    @GetMapping("/summary")
    public ResponseEntity<PolicySummaryStats> getSummaryStats() {
        log.debug("GET /api/v1/policies/summary");
        return ResponseEntity.ok(policyMapper.toStats(policyService.getSummary()));
    }
}
