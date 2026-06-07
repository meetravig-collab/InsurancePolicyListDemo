package com.insurance.dashboard.infrastructure.persistence;

import com.insurance.dashboard.domain.model.Policy;
import com.insurance.dashboard.domain.model.Policy.LineOfBusiness;
import com.insurance.dashboard.domain.model.Policy.PolicyStatus;
import com.insurance.dashboard.domain.model.Policy.Region;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class PolicySpecification {

    private PolicySpecification() {}

    public static Specification<Policy> withFilters(PolicyStatus status,
                                                    Region region,
                                                    LineOfBusiness lineOfBusiness,
                                                    LocalDate effectiveDateFrom,
                                                    LocalDate effectiveDateTo,
                                                    String search) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (region != null) {
                predicates.add(cb.equal(root.get("region"), region));
            }
            if (lineOfBusiness != null) {
                predicates.add(cb.equal(root.get("lineOfBusiness"), lineOfBusiness));
            }
            if (effectiveDateFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("effectiveDate"), effectiveDateFrom));
            }
            if (effectiveDateTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("effectiveDate"), effectiveDateTo));
            }
            // free-text search across policyNumber, policyholderName, underwriter
            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("policyNumber")), pattern),
                        cb.like(cb.lower(root.get("policyholderName")), pattern),
                        cb.like(cb.lower(root.get("underwriter")), pattern)
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
