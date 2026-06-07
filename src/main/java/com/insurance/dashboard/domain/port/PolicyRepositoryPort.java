package com.insurance.dashboard.domain.port;

import com.insurance.dashboard.domain.model.Policy;
import com.insurance.dashboard.domain.model.Policy.LineOfBusiness;
import com.insurance.dashboard.domain.model.Policy.PolicyStatus;
import com.insurance.dashboard.domain.query.PageQuery;
import com.insurance.dashboard.domain.query.PageResult;
import com.insurance.dashboard.domain.query.PolicyFilter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Outbound port owned by the domain/application. The persistence adapter in the
 * infrastructure layer implements it. The service depends only on this abstraction,
 * never on Spring Data or JPA types.
 */
public interface PolicyRepositoryPort {

    PageResult<Policy> findAll(PolicyFilter filter, PageQuery page);

    Optional<Policy> findById(UUID id);

    int flagForReview(List<UUID> ids);

    Map<PolicyStatus, Long> countByStatus();

    Map<LineOfBusiness, BigDecimal> totalPremiumByLineOfBusiness();

    long countExpiringSoon(PolicyStatus status, LocalDate from, LocalDate toExclusive);
}
