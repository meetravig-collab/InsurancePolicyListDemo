package com.insurance.dashboard.infrastructure.persistence.repository;

import com.insurance.dashboard.domain.model.Policy.PolicyStatus;
import com.insurance.dashboard.infrastructure.persistence.entity.PolicyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface PolicyJpaRepository extends JpaRepository<PolicyEntity, UUID>, JpaSpecificationExecutor<PolicyEntity> {

    @Modifying(clearAutomatically = true)
    @Query("UPDATE PolicyEntity p SET p.flaggedForReview = true WHERE p.id IN :ids")
    int bulkFlagForReview(@Param("ids") List<UUID> ids);

    @Query("SELECT p.status, COUNT(p) FROM PolicyEntity p GROUP BY p.status")
    List<Object[]> countGroupByStatus();

    @Query("SELECT p.lineOfBusiness, SUM(p.premiumAmount) FROM PolicyEntity p WHERE p.lineOfBusiness IS NOT NULL GROUP BY p.lineOfBusiness")
    List<Object[]> sumPremiumGroupByLineOfBusiness();

    @Query("SELECT COUNT(p) FROM PolicyEntity p WHERE p.status = :status AND p.expiryDate >= :today AND p.expiryDate < :cutoffDate")
    long countExpiringSoon(@Param("status") PolicyStatus status,
                           @Param("today") LocalDate today,
                           @Param("cutoffDate") LocalDate cutoffDate);
}
