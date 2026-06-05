package com.insurance.dashboard.repository;

import com.insurance.dashboard.model.Policy;
import com.insurance.dashboard.model.Policy.PolicyStatus;
import com.insurance.dashboard.model.Policy.PolicyType;
import com.insurance.dashboard.model.Policy.Region;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PolicyRepository extends JpaRepository<Policy, Long> {

    Optional<Policy> findByPolicyNumber(String policyNumber);

    List<Policy> findByPolicyHolderId(Long policyHolderId);

    List<Policy> findByStatus(PolicyStatus status);

    List<Policy> findByPolicyType(PolicyType policyType);

    @Query("SELECT p FROM Policy p JOIN FETCH p.policyHolder WHERE (:status IS NULL OR p.status = :status) AND (:region IS NULL OR p.region = :region)")
    Page<Policy> findAllWithFilters(@Param("status") PolicyStatus status, @Param("region") Region region, Pageable pageable);
}
