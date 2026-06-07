package com.insurance.dashboard.infrastructure.persistence.repository;

import com.insurance.dashboard.domain.model.PolicyHolder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PolicyHolderRepository extends JpaRepository<PolicyHolder, Long> {

    Optional<PolicyHolder> findByEmail(String email);

    List<PolicyHolder> findByLastNameContainingIgnoreCase(String lastName);
}
