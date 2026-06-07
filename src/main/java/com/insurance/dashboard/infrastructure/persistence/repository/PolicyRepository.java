package com.insurance.dashboard.infrastructure.persistence.repository;

import com.insurance.dashboard.domain.model.Policy;
import com.insurance.dashboard.domain.model.Policy.PolicyStatus;
import com.insurance.dashboard.domain.model.Policy.Region;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PolicyRepository extends JpaRepository<Policy, Long> {

    @Query("SELECT p FROM Policy p JOIN FETCH p.policyHolder WHERE (:status IS NULL OR p.status = :status) AND (:region IS NULL OR p.region = :region)")
    Page<Policy> findAllWithFilters(@Param("status") PolicyStatus status, @Param("region") Region region, Pageable pageable);
}
