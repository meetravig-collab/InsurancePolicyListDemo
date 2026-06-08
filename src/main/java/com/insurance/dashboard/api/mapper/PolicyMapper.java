package com.insurance.dashboard.api.mapper;

import com.insurance.dashboard.api.dto.response.PolicySummaryResponse;
import com.insurance.dashboard.api.dto.response.PolicySummaryStats;
import com.insurance.dashboard.domain.model.Policy;
import com.insurance.dashboard.domain.query.PageQuery;
import com.insurance.dashboard.service.PolicySummary;
import org.springframework.data.domain.Pageable;

/** Translates between API/Spring types and domain types. */
public interface PolicyMapper {

    PolicySummaryResponse toResponse(Policy policy);

    PolicySummaryStats toStats(PolicySummary summary);

    PageQuery toPageQuery(Pageable pageable);
}
