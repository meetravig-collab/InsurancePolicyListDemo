package com.insurance.dashboard.infrastructure.persistence;

import com.insurance.dashboard.domain.model.Policy;
import com.insurance.dashboard.domain.model.Policy.LineOfBusiness;
import com.insurance.dashboard.domain.model.Policy.PolicyStatus;
import com.insurance.dashboard.domain.port.PolicyRepositoryPort;
import com.insurance.dashboard.domain.query.PageQuery;
import com.insurance.dashboard.domain.query.PageResult;
import com.insurance.dashboard.domain.query.PolicyFilter;
import com.insurance.dashboard.domain.query.SortDirection;
import com.insurance.dashboard.infrastructure.persistence.entity.PolicyEntity;
import com.insurance.dashboard.infrastructure.persistence.entity.PolicyEntityMapper;
import com.insurance.dashboard.infrastructure.persistence.repository.PolicyJpaRepository;
import com.insurance.dashboard.infrastructure.persistence.specification.PolicySpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Adapter implementing the domain {@link PolicyRepositoryPort} using Spring Data JPA.
 * All persistence/framework types are confined to this class.
 */
@Component
@RequiredArgsConstructor
public class PolicyPersistenceAdapter implements PolicyRepositoryPort {

    private final PolicyJpaRepository jpaRepository;
    private final PolicyEntityMapper mapper;

    @Override
    public PageResult<Policy> findAll(PolicyFilter filter, PageQuery page) {
        Sort sort = Sort.by(
                page.direction() == SortDirection.DESC ? Sort.Direction.DESC : Sort.Direction.ASC,
                page.sortField() != null ? page.sortField() : "effectiveDate");
        PageRequest pageRequest = PageRequest.of(page.page(), page.size(), sort);

        Page<PolicyEntity> result = jpaRepository.findAll(PolicySpecification.fromFilter(filter), pageRequest);
        List<Policy> content = result.getContent().stream().map(mapper::toDomain).toList();
        return new PageResult<>(content, result.getTotalElements(), page.page(), page.size());
    }

    @Override
    public Optional<Policy> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public int flagForReview(List<UUID> ids) {
        return jpaRepository.bulkFlagForReview(ids);
    }

    @Override
    public Map<PolicyStatus, Long> countByStatus() {
        Map<PolicyStatus, Long> result = new LinkedHashMap<>();
        jpaRepository.countGroupByStatus()
                .forEach(row -> result.put((PolicyStatus) row[0], (Long) row[1]));
        return result;
    }

    @Override
    public Map<LineOfBusiness, BigDecimal> totalPremiumByLineOfBusiness() {
        Map<LineOfBusiness, BigDecimal> result = new LinkedHashMap<>();
        jpaRepository.sumPremiumGroupByLineOfBusiness()
                .forEach(row -> result.put((LineOfBusiness) row[0], (BigDecimal) row[1]));
        return result;
    }

    @Override
    public long countExpiringSoon(PolicyStatus status, LocalDate from, LocalDate toExclusive) {
        return jpaRepository.countExpiringSoon(status, from, toExclusive);
    }
}
